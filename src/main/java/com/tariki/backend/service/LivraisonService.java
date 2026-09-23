package com.tariki.backend.service;

import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.dto.NavigationDTO;
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
    private final LivePositionRepository positions;
    private final DeliveryInvitationService invitations;
    private final UserRepository users;

    public LivraisonService(LivraisonRepository repository, LivraisonMapper mapper, ChauffeurRepository chauffeurs,
                            CamionRepository camions, ClientRepository clients, AccessScope scope, LivePositionRepository positions,
                            DeliveryInvitationService invitations, UserRepository users) {
        this.repository = repository;
        this.mapper = mapper;
        this.chauffeurs = chauffeurs;
        this.camions = camions;
        this.clients = clients;
        this.scope = scope;
        this.positions = positions;
        this.invitations = invitations;
        this.users = users;
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
        if (dto.getId() != null) repository.lockRow(dto.getId());
        Livraison existing = dto.getId() == null ? null : visible(dto.getId());
        Entreprise entreprise = existing == null ? scope.managedCompany() : existing.getEntreprise();
        scope.requireCompany(entreprise);
        if (dto.getChauffeurId() == null || dto.getCamionId() == null || (dto.getClientId() == null && dto.getNouveauClient() == null)
                || dto.getDateLivraison() == null || dto.getPoidsTonnes() == null || dto.getPoidsTonnes().signum() <= 0
                || blank(dto.getVilleDepart()) || blank(dto.getVilleArrivee()) || blank(dto.getMarchandise())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez le trajet, la date, le chargement et les affectations");
        }
        Chauffeur chauffeur = chauffeurs.findById(dto.getChauffeurId()).orElseThrow(this::invalidAssignment);
        Camion camion = camions.findById(dto.getCamionId()).orElseThrow(this::invalidAssignment);
        if (dto.getNouveauClient() != null && dto.getClientId() != null) throw invalidAssignment();
        Client client = dto.getNouveauClient() != null ? invitations.createClient(dto.getNouveauClient(), entreprise)
                : clients.findById(dto.getClientId()).orElseThrow(this::invalidAssignment);
        if (!sameCompany(entreprise, chauffeur.getEntreprise()) || !sameCompany(entreprise, camion.getEntreprise())
                || !sameCompany(entreprise, client.getEntreprise())) throw invalidAssignment();
        if (dto.getPoidsTonnes().doubleValue() > camion.getCapacite()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le chargement depasse la capacite du camion");
        }
        if (existing != null && !"PROGRAMMEE".equals(existing.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une livraison programmee peut etre modifiee");
        }
        Livraison livraison = existing == null ? new Livraison() : existing;
        String reference = blank(dto.getReference()) ? "TRK-" + java.time.LocalDate.now() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase() : dto.getReference().trim();
        if (reference.length() > 100 || dto.getVilleDepart().length() > 100 || dto.getVilleArrivee().length() > 100 || dto.getMarchandise().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un des champs est trop long");
        }
        if ((existing == null || !reference.equalsIgnoreCase(existing.getReference()))
                && repository.existsByReferenceIgnoreCaseAndEntrepriseId(reference, entreprise.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette reference existe deja");
        }
        if (existing != null && !Objects.equals(dto.getVersion(), existing.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La livraison a change. Actualisez la page.");
        }
        DeliveryCompletionService.validatePricing(dto.getServiceFacture(), dto.getPrixHT(), dto.getTauxTVA());
        livraison.setServiceFacture(dto.getServiceFacture().trim());
        livraison.setPrixHT(dto.getPrixHT());
        livraison.setTauxTVA(dto.getTauxTVA());
        if (existing == null || !Objects.equals(existing.getClient().getId(), client.getId())) invitations.initialize(livraison);
        livraison.setReference(reference);
        livraison.setDateLivraison(dto.getDateLivraison());
        livraison.setVilleDepart(dto.getVilleDepart().trim());
        livraison.setVilleArrivee(dto.getVilleArrivee().trim());
        if (dto.getAdresseLivraison() != null || dto.getDestinationLatitude() != null || dto.getDestinationLongitude() != null) {
            setDestination(livraison, dto.getAdresseLivraison(), dto.getDestinationLatitude(), dto.getDestinationLongitude());
        }
        livraison.setMarchandise(dto.getMarchandise().trim());
        livraison.setPoidsTonnes(dto.getPoidsTonnes());
        livraison.setChauffeur(chauffeur);
        livraison.setCamion(camion);
        livraison.setClient(client);
        livraison.setEntreprise(entreprise);
        livraison.setStatut("PROGRAMMEE");
        livraison.setDernierePosition(livraison.getVilleDepart());
        livraison.setMiseAJour(LocalDateTime.now());
        return mapper.toDTO(repository.saveAndFlush(livraison));
    }

    public LivraisonDTO changeStatus(Long id, String statut) {
        repository.lockRow(id);
        Livraison livraison = visible(id);
        users.lockForTracking(livraison.getChauffeur().getId());
        if (scope.currentUser().getRole() == User.Role.CLIENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if ("EN_ATTENTE_VALIDATION".equals(statut) && scope.currentUser().getRole() != User.Role.CHAUFFEUR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul le chauffeur affecte peut signaler l'arrivee");
        }
        String previous = livraison.getStatut();
        boolean allowed = ("PROGRAMMEE".equals(previous) && "EN_COURS".equals(statut))
                || (("EN_COURS".equals(previous) || "DEMARRE".equals(previous)) && "EN_ATTENTE_VALIDATION".equals(statut));
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "Transition de statut impossible");
        if ("EN_COURS".equals(statut) && repository.findAll().stream().anyMatch(other -> !Objects.equals(id, other.getId())
                && ("EN_COURS".equals(other.getStatut()) || "DEMARRE".equals(other.getStatut()))
                && ((other.getChauffeur() != null && Objects.equals(other.getChauffeur().getId(), livraison.getChauffeur().getId()))
                || (other.getCamion() != null && Objects.equals(other.getCamion().getId(), livraison.getCamion().getId()))))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le chauffeur ou le camion a deja une livraison en cours");
        }
        livraison.setStatut(statut);
        if ("EN_ATTENTE_VALIDATION".equals(statut)) {
            positions.stopForDelivery(id);
            livraison.setArriveeAt(java.time.Instant.now());
        }
        livraison.setDernierePosition("EN_ATTENTE_VALIDATION".equals(statut) ? livraison.getVilleArrivee() : livraison.getVilleDepart());
        livraison.setMiseAJour(LocalDateTime.now());
        return mapper.toDTO(repository.saveAndFlush(livraison));
    }

    public LivraisonDTO updateDestination(Long id, NavigationDTO.Destination destination) {
        repository.lockRow(id);
        Livraison livraison = visible(id);
        scope.requireCompany(livraison.getEntreprise());
        if (!List.of("PROGRAMMEE", "EN_COURS", "DEMARRE").contains(livraison.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La destination d'une livraison arrivee ne peut plus etre modifiee");
        }
        if (!Objects.equals(destination.version(), livraison.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La livraison a change. Actualisez la page.");
        }
        setDestination(livraison, destination.adresseLivraison(), destination.destinationLatitude(), destination.destinationLongitude());
        livraison.setMiseAJour(LocalDateTime.now());
        return mapper.toDTO(repository.saveAndFlush(livraison));
    }

    @Transactional(readOnly = true)
    public NavigationDTO.Target navigationTarget(Long id) {
        User viewer = scope.currentUser();
        if (viewer.getRole() != User.Role.CHAUFFEUR) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Livraison livraison = visible(id);
        if (!sameCompany(viewer.getEntreprise(), livraison.getEntreprise())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!List.of("EN_COURS", "DEMARRE").contains(livraison.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le guidage exige une livraison en cours");
        }
        if (!NavigationDTO.coordinates(livraison.getDestinationLatitude(), livraison.getDestinationLongitude())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le point de livraison doit etre renseigne par l'entreprise");
        }
        return new NavigationDTO.Target(viewer.getId(), livraison.getDestinationLatitude(), livraison.getDestinationLongitude());
    }

    private void setDestination(Livraison livraison, String address, Double latitude, Double longitude) {
        if (blank(address) || address.length() > 500 || !NavigationDTO.coordinates(latitude, longitude)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez une adresse et un point GPS valides pour la livraison");
        }
        livraison.setAdresseLivraison(address.trim());
        livraison.setDestinationLatitude(latitude);
        livraison.setDestinationLongitude(longitude);
    }

    public void delete(Long id) {
        repository.lockRow(id);
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
