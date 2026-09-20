package com.tariki.backend.service;

import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.mapper.LivraisonMapper;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.security.AccessScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class LivraisonService {
    private final LivraisonRepository repository;
    private final LivraisonMapper mapper;
    private final ChauffeurRepository chauffeurs;
    private final CamionRepository camions;
    private final ClientRepository clients;
    private final AccessScope scope;

    public LivraisonService(LivraisonRepository repository, LivraisonMapper mapper, ChauffeurRepository chauffeurs,
                            CamionRepository camions, ClientRepository clients, AccessScope scope) {
        this.repository = repository;
        this.mapper = mapper;
        this.chauffeurs = chauffeurs;
        this.camions = camions;
        this.clients = clients;
        this.scope = scope;
    }

    public List<LivraisonDTO> findAll() {
        User viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.delivery(viewer, item)).map(mapper::toDTO).toList();
    }

    public LivraisonDTO findById(Long id) { return mapper.toDTO(visible(id)); }

    private Livraison visible(Long id) {
        Livraison livraison = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livraison introuvable"));
        scope.requireDelivery(livraison);
        return livraison;
    }

    public LivraisonDTO save(LivraisonDTO dto) {
        Livraison existing = dto.getId() == null ? null : visible(dto.getId());
        Entreprise entreprise = existing == null ? scope.managedCompany() : existing.getEntreprise();
        scope.requireCompany(entreprise);
        if (dto.getChauffeurId() == null || dto.getCamionId() == null || dto.getClientId() == null
                || dto.getDateLivraison() == null || dto.getPoidsTonnes() == null || dto.getPoidsTonnes().signum() <= 0
                || blank(dto.getReference()) || blank(dto.getVilleDepart()) || blank(dto.getVilleArrivee()) || blank(dto.getMarchandise())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez le trajet, la date, le chargement et les affectations");
        }
        Chauffeur chauffeur = chauffeurs.findById(dto.getChauffeurId()).orElseThrow(this::invalidAssignment);
        Camion camion = camions.findById(dto.getCamionId()).orElseThrow(this::invalidAssignment);
        Client client = clients.findById(dto.getClientId()).orElseThrow(this::invalidAssignment);
        if (!sameCompany(entreprise, chauffeur.getEntreprise()) || !sameCompany(entreprise, camion.getEntreprise())
                || !sameCompany(entreprise, client.getEntreprise())) throw invalidAssignment();
        if (dto.getPoidsTonnes().doubleValue() > camion.getCapacite()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le chargement depasse la capacite du camion");
        }
        if (existing != null && !"PROGRAMMEE".equals(existing.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une livraison programmee peut etre modifiee");
        }
        Livraison livraison = existing == null ? new Livraison() : existing;
        livraison.setReference(dto.getReference().trim());
        livraison.setDateLivraison(dto.getDateLivraison());
        livraison.setVilleDepart(dto.getVilleDepart().trim());
        livraison.setVilleArrivee(dto.getVilleArrivee().trim());
        livraison.setMarchandise(dto.getMarchandise().trim());
        livraison.setPoidsTonnes(dto.getPoidsTonnes());
        livraison.setChauffeur(chauffeur);
        livraison.setCamion(camion);
        livraison.setClient(client);
        livraison.setEntreprise(entreprise);
        livraison.setStatut("PROGRAMMEE");
        livraison.setDernierePosition(livraison.getVilleDepart());
        livraison.setMiseAJour(LocalDateTime.now());
        return mapper.toDTO(repository.save(livraison));
    }

    public LivraisonDTO changeStatus(Long id, String statut) {
        Livraison livraison = visible(id);
        String previous = livraison.getStatut();
        boolean allowed = ("PROGRAMMEE".equals(previous) && "EN_COURS".equals(statut))
                || (("EN_COURS".equals(previous) || "DEMARRE".equals(previous)) && "LIVREE".equals(statut));
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transition de statut impossible");
        if ("EN_COURS".equals(statut) && repository.findAll().stream().anyMatch(other -> !Objects.equals(id, other.getId())
                && ("EN_COURS".equals(other.getStatut()) || "DEMARRE".equals(other.getStatut()))
                && ((other.getChauffeur() != null && Objects.equals(other.getChauffeur().getId(), livraison.getChauffeur().getId()))
                || (other.getCamion() != null && Objects.equals(other.getCamion().getId(), livraison.getCamion().getId()))))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le chauffeur ou le camion a deja une livraison en cours");
        }
        livraison.setStatut(statut);
        livraison.setDernierePosition("LIVREE".equals(statut) ? livraison.getVilleArrivee() : livraison.getVilleDepart());
        livraison.setMiseAJour(LocalDateTime.now());
        return mapper.toDTO(repository.save(livraison));
    }

    public void delete(Long id) {
        Livraison livraison = visible(id);
        scope.requireCompany(livraison.getEntreprise());
        if (!"PROGRAMMEE".equals(livraison.getStatut())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Livraison deja demarree");
        repository.delete(livraison);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private boolean sameCompany(Entreprise a, Entreprise b) { return a != null && b != null && Objects.equals(a.getId(), b.getId()); }
    private ResponseStatusException invalidAssignment() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les affectations doivent appartenir a la meme entreprise");
    }
}
