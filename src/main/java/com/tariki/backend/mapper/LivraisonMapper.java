package com.tariki.backend.mapper;

import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.model.Chauffeur;
import com.tariki.backend.model.Camion;
import com.tariki.backend.model.Client;
import org.springframework.stereotype.Component;

@Component
public class LivraisonMapper {
    public LivraisonDTO toDTO(Livraison livraison) {
        LivraisonDTO dto = new LivraisonDTO();
        dto.setId(livraison.getId());
        dto.setReference(livraison.getReference());
        dto.setDateLivraison(livraison.getDateLivraison());
        dto.setStatut(livraison.getStatut());
        dto.setChauffeurId(livraison.getChauffeur() != null ? livraison.getChauffeur().getId() : null);
        dto.setCamionId(livraison.getCamion() != null ? livraison.getCamion().getId() : null);
        dto.setClientId(livraison.getClient() != null ? livraison.getClient().getId() : null);
        dto.setEntrepriseId(livraison.getEntreprise() != null ? livraison.getEntreprise().getId() : null);
        dto.setEntrepriseNom(livraison.getEntreprise() != null ? livraison.getEntreprise().getNom() : null);
        dto.setChauffeurNom(livraison.getChauffeur() != null ? livraison.getChauffeur().getPrenom() + " " + livraison.getChauffeur().getNom() : null);
        dto.setCamionImmatriculation(livraison.getCamion() != null ? livraison.getCamion().getImmatriculation() : null);
        dto.setClientNom(livraison.getClient() != null ? livraison.getClient().getNom() : null);
        dto.setVilleDepart(livraison.getVilleDepart());
        dto.setVilleArrivee(livraison.getVilleArrivee());
        dto.setAdresseLivraison(livraison.getAdresseLivraison());
        dto.setDestinationLatitude(livraison.getDestinationLatitude());
        dto.setDestinationLongitude(livraison.getDestinationLongitude());
        dto.setMarchandise(livraison.getMarchandise());
        dto.setPoidsTonnes(livraison.getPoidsTonnes());
        dto.setDernierePosition(livraison.getDernierePosition());
        dto.setMiseAJour(livraison.getMiseAJour());
        dto.setVersion(livraison.getVersion());
        dto.setServiceFacture(livraison.getServiceFacture());
        dto.setPrixHT(livraison.getPrixHT());
        dto.setTauxTVA(livraison.getTauxTVA());
        if (livraison.getPrixHT() != null && livraison.getTauxTVA() != null) {
            var tax = livraison.getPrixHT().multiply(livraison.getTauxTVA()).divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            dto.setMontantTVA(tax);
            dto.setMontantTTC(livraison.getPrixHT().add(tax));
        }
        dto.setArriveeAt(livraison.getArriveeAt());
        dto.setValidationAt(livraison.getValidationAt());
        dto.setClientInvitationPending(livraison.getClient() != null && livraison.getClient().isInvitationPending());
        return dto;
    }
    public Livraison toEntity(LivraisonDTO dto, Chauffeur chauffeur, Camion camion, Client client) {
        return Livraison.builder()
                .id(dto.getId())
                .reference(dto.getReference())
                .dateLivraison(dto.getDateLivraison())
                .statut(dto.getStatut())
                .chauffeur(chauffeur)
                .camion(camion)
                .client(client)
                .villeDepart(dto.getVilleDepart())
                .villeArrivee(dto.getVilleArrivee())
                .adresseLivraison(dto.getAdresseLivraison())
                .destinationLatitude(dto.getDestinationLatitude())
                .destinationLongitude(dto.getDestinationLongitude())
                .marchandise(dto.getMarchandise())
                .poidsTonnes(dto.getPoidsTonnes())
                .build();
    }
}
