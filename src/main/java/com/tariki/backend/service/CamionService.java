package com.tariki.backend.service;

import com.tariki.backend.dto.*;
import com.tariki.backend.mapper.CamionMapper;
import com.tariki.backend.model.Camion;
import com.tariki.backend.model.CamionEntretien;
import com.tariki.backend.repository.*;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CamionService {
    private final AccessScope scope;
    private final CamionRepository repository;
    private final CamionMapper mapper;
    private final CamionEntretienRepository entretiens;
    private final LivraisonRepository livraisons;

    public CamionService(CamionRepository repository, CamionMapper mapper, AccessScope scope,
                         CamionEntretienRepository entretiens, LivraisonRepository livraisons) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
        this.entretiens = entretiens;
        this.livraisons = livraisons;
    }

    public List<CamionDTO> findAll() {
        var viewer = scope.currentUser();
        var trucks = repository.findAll().stream().filter(item -> scope.company(viewer, item.getEntreprise())).toList();
        if (trucks.isEmpty()) return List.of();
        var pending = entretiens.findByCamionIdInAndEffectueLeIsNull(trucks.stream().map(Camion::getId).toList())
                .stream().collect(Collectors.groupingBy(item -> item.getCamion().getId()));
        return trucks.stream().map(truck -> response(truck, pending.getOrDefault(truck.getId(), List.of()))).toList();
    }

    public CamionDTO findById(Long id) {
        Camion truck = requireTruck(id);
        return response(truck, entretiens.findByCamionIdOrderByIdDesc(id));
    }

    Camion requireTruck(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.company(viewer, item.getEntreprise()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camion introuvable"));
    }

    public CamionDTO save(CamionDTO dto) {
        Camion truck;
        if (dto.getId() == null) {
            truck = new Camion();
            truck.setEntreprise(scope.managedCompany());
        } else {
            truck = requireTruck(dto.getId());
            if (!Objects.equals(dto.getVersion(), truck.getVersion())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce camion a ete modifie. Rechargez sa fiche.");
            }
            if (truck.getKilometrage() != null && (dto.getKilometrage() == null || dto.getKilometrage() < truck.getKilometrage())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le kilometrage ne peut pas diminuer");
            }
            if (dto.getCapacite() < truck.getCapacite() && livraisons.existsByCamionIdAndStatutInAndPoidsTonnesGreaterThan(
                    truck.getId(), List.of("PROGRAMMEE", "EN_COURS", "DEMARRE", "EN_ATTENTE_VALIDATION"), java.math.BigDecimal.valueOf(dto.getCapacite()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La capacite est inferieure au chargement d'une livraison affectee");
            }
        }
        if ((dto.getScorePneus() == null) != (dto.getControlePneusLe() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez le score des pneus et la date du controle ensemble");
        }
        mapper.update(dto, truck);
        truck = repository.saveAndFlush(truck);
        return findById(truck.getId());
    }

    public byte[] photo(Long id) {
        byte[] bytes = requireTruck(id).getPhotoJpeg();
        if (bytes == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune photo pour ce camion");
        return bytes;
    }

    public CamionDTO photo(Long id, String image, Long version) {
        Camion truck = requireTruck(id);
        if (!Objects.equals(version, truck.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce camion a ete modifie. Rechargez sa fiche.");
        }
        truck.setPhotoJpeg(image == null ? null : DocumentImages.decodePhoto(image));
        repository.flush();
        return findById(id);
    }

    public void delete(Long id) {
        requireTruck(id);
        if (livraisons.existsByCamionId(id) || entretiens.existsByCamionId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce camion possede des livraisons ou un historique d'entretien");
        }
        repository.deleteById(id);
    }

    private CamionDTO response(Camion truck, List<CamionEntretien> tasks) {
        CamionDTO dto = mapper.toDTO(truck);
        var statuses = tasks.stream().map(task -> CamionEntretienDTO.from(task, truck.getKilometrage(), LocalDate.now()).statut()).toList();
        dto.setEntretiensUrgents((int) statuses.stream().filter("A_FAIRE"::equals).count());
        dto.setEntretiensProches((int) statuses.stream().filter("PROCHE"::equals).count());
        return dto;
    }
}
