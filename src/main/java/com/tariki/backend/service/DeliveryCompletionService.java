package com.tariki.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariki.backend.dto.DeliveryWorkflowDTO.*;
import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.mapper.LivraisonMapper;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.security.AccessScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class DeliveryCompletionService {
    private final LivraisonRepository deliveries;
    private final FactureRepository invoices;
    private final SignatureRepository signatures;
    private final LivePositionRepository positions;
    private final AccessScope scope;
    private final LivraisonMapper mapper;
    private final InvoicePdf pdf;
    private final ObjectMapper json;

    public DeliveryCompletionService(LivraisonRepository deliveries, FactureRepository invoices, SignatureRepository signatures,
                                     LivePositionRepository positions, AccessScope scope, LivraisonMapper mapper, InvoicePdf pdf, ObjectMapper json) {
        this.deliveries=deliveries; this.invoices=invoices; this.signatures=signatures; this.positions=positions;
        this.scope=scope; this.mapper=mapper; this.pdf=pdf; this.json=json;
    }

    private Livraison visible(Long id, boolean lock) {
        if (lock) deliveries.lockRow(id);
        Livraison d = deliveries.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        scope.requireDelivery(d); return d;
    }

    public LivraisonDTO pricing(Long id, Pricing request) {
        Livraison d = visible(id,true); scope.requireCompany(d.getEntreprise());
        if ("LIVREE".equals(d.getStatut()) || "ANNULEE".equals(d.getStatut())) throw conflict("Livraison cloturee");
        checkVersion(d,request.version());
        validatePricing(request.serviceFacture(), request.prixHT(), request.tauxTVA());
        d.setServiceFacture(request.serviceFacture().trim()); d.setPrixHT(request.prixHT()); d.setTauxTVA(request.tauxTVA());
        return mapper.toDTO(deliveries.saveAndFlush(d));
    }

    static void validatePricing(String service, BigDecimal price, BigDecimal vat) {
        if (service == null || service.isBlank() || service.length()>255 || price == null || price.signum()<=0
                || price.compareTo(new BigDecimal("9999999999.99"))>0 || price.scale()>2
                || vat == null || vat.signum()<0 || vat.compareTo(new BigDecimal("100"))>0 || vat.scale()>2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Renseignez le service, le prix HT en MAD et le taux de TVA (0 a 100 %), avec au maximum 2 decimales");
        }
    }

    public LivraisonDTO receive(Long id, Receipt request) {
        Livraison d = visible(id,true);
        User client = scope.currentUser();
        if (client.getRole() != User.Role.CLIENT || !Objects.equals(client.getId(),d.getClient().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le client rattache peut valider la reception");
        }
        if ("LIVREE".equals(d.getStatut()) && d.getValidationAt()!=null && invoices.findByLivraisonId(id).isPresent()) return mapper.toDTO(d);
        if (!"EN_ATTENTE_VALIDATION".equals(d.getStatut())) throw conflict("Le chauffeur doit d'abord signaler l'arrivee");
        checkVersion(d,request.version());
        if (!request.consentement()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Consentement obligatoire");
        validatePricing(d.getServiceFacture(),d.getPrixHT(),d.getTauxTVA());
        Entreprise company=d.getEntreprise();
        if (blank(company.getNom()) || blank(company.getAdresse()) || blank(company.getEmail()) || blank(company.getTelephone())
                || blank(d.getClient().getAdresse())) throw conflict("L'entreprise doit completer les coordonnees de facturation avant la validation");
        if (invoices.findByLivraisonId(id).isPresent()) throw conflict("Une facture existe deja pour cette livraison");
        byte[] image=DocumentImages.decode(request.signature(),true);
        Instant now=Instant.now();
        Signature signature=signatures.save(Signature.builder().livraison(d).signataire(request.signataire().trim()).type("RECEPTION_CLIENT")
                .dateSignature(LocalDateTime.ofInstant(now, ZoneOffset.UTC)).validationAt(now).utilisateurId(client.getId())
                .consentementVersion("reception-v1").imagePng(image).build());
        d.setStatut("LIVREE"); d.setValidationAt(now); d.setMiseAJour(LocalDateTime.now());
        positions.stopForDelivery(id);
        BigDecimal ht=d.getPrixHT().setScale(2,RoundingMode.HALF_UP);
        BigDecimal vat=ht.multiply(d.getTauxTVA()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        Facture invoice=invoices.saveAndFlush(Facture.builder().livraison(d).montantHT(ht).montantTVA(vat).montantTTC(ht.add(vat))
                .tauxTVA(d.getTauxTVA()).emiseAt(now).build());
        invoice.setNumero("FAC-"+LocalDate.ofInstant(now,ZoneId.of("Africa/Casablanca")).getYear()+"-"+String.format(Locale.ROOT,"%06d",invoice.getId()));
        var snapshot=new LinkedHashMap<String,Object>();
        snapshot.put("livraison",mapper.toDTO(d));
        snapshot.put("entreprise",new com.tariki.backend.mapper.EntrepriseMapper().toDTO(company));
        snapshot.put("client",Map.of("nom",d.getClient().getNom(),"prenom",Objects.toString(d.getClient().getPrenom(),""),
                "email",Objects.toString(d.getClient().getEmail(),""),"telephone",Objects.toString(d.getClient().getTelephone(),""),"adresse",d.getClient().getAdresse()));
        // The PDF itself embeds the original logo; keep the textual snapshot compact.
        ((com.tariki.backend.dto.EntrepriseDTO)snapshot.get("entreprise")).setLogo(null);
        try { invoice.setInstantane(json.writeValueAsString(snapshot)); }
        catch (JsonProcessingException exception) { throw new IllegalStateException(exception); }
        invoice.setPdf(pdf.generate(invoice,signature));
        invoices.saveAndFlush(invoice);
        return mapper.toDTO(deliveries.saveAndFlush(d));
    }

    @Transactional(readOnly=true)
    public Documents documents(Long id) {
        Livraison d=visible(id,false);
        Facture f=invoices.findByLivraisonId(id).orElse(null);
        Signature s=signatures.findFirstByLivraisonIdAndType(id,"RECEPTION_CLIENT").orElse(null);
        boolean financial=scope.currentUser().getRole()!=User.Role.CHAUFFEUR;
        return new Documents(financial && f!=null ? f.getId():null, financial && f!=null ? f.getNumero():null,
                financial && f!=null && f.getPdf()!=null, s==null?null:s.getSignataire(),d.getValidationAt());
    }

    private static boolean blank(String s) { return s==null || s.isBlank(); }
    private void checkVersion(Livraison d,Long version) {
        if (!Objects.equals(d.getVersion(),version)) throw conflict("La livraison ou son tarif a change. Actualisez avant de confirmer.");
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
