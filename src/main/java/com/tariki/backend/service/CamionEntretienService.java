package com.tariki.backend.service;

import com.tariki.backend.dto.*;
import com.tariki.backend.model.Camion;
import com.tariki.backend.model.CamionEntretien;
import com.tariki.backend.repository.CamionEntretienRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class CamionEntretienService {
    private final CamionService trucks;
    private final CamionEntretienRepository repository;

    public CamionEntretienService(CamionService trucks, CamionEntretienRepository repository) {
        this.trucks = trucks;
        this.repository = repository;
    }

    public List<CamionEntretienDTO> findAll(Long truckId) {
        Camion truck = trucks.requireTruck(truckId);
        return repository.findByCamionIdOrderByIdDesc(truckId).stream().map(item -> response(item, truck)).toList();
    }

    public CamionEntretienDTO save(Long truckId, Long id, CamionEntretienRequest request) {
        Camion truck = trucks.requireTruck(truckId);
        CamionEntretien task = id == null ? new CamionEntretien() : pending(truckId, id, request.version());
        if (request.echeanceDate() == null && request.echeanceKm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseignez une date ou un kilometrage d'echeance");
        }
        if (request.type() == CamionEntretien.Type.VIDANGE && truck.getCarburant() == Camion.Carburant.ELECTRIQUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La vidange moteur ne s'applique pas a un camion electrique");
        }
        task.setCamion(truck);
        task.setType(request.type());
        task.setEcheanceDate(request.echeanceDate());
        task.setEcheanceKm(request.echeanceKm());
        task.setNotes(request.notes());
        return response(repository.saveAndFlush(task), truck);
    }

    public CamionEntretienDTO complete(Long truckId, Long id, CamionEntretienCompletion request) {
        Camion truck = trucks.requireTruck(truckId);
        CamionEntretien task = pending(truckId, id, request.version());
        if (request.scorePneus() != null && task.getType() != CamionEntretien.Type.PNEUS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le score concerne uniquement un controle des pneus");
        }
        task.setEffectueLe(request.effectueLe());
        task.setEffectueKm(request.effectueKm());
        task.setScorePneus(request.scorePneus());
        if (truck.getKilometrage() == null || truck.getKilometrage() < request.effectueKm()) truck.setKilometrage(request.effectueKm());
        if (request.scorePneus() != null && (truck.getControlePneusLe() == null || !request.effectueLe().isBefore(truck.getControlePneusLe()))) {
            truck.setScorePneus(request.scorePneus());
            truck.setControlePneusLe(request.effectueLe());
        }
        repository.flush();
        return response(task, truck);
    }

    public void delete(Long truckId, Long id, Long version) {
        trucks.requireTruck(truckId);
        repository.delete(pending(truckId, id, version));
    }

    private CamionEntretien pending(Long truckId, Long id, Long version) {
        CamionEntretien task = repository.findById(id).filter(item -> Objects.equals(item.getCamion().getId(), truckId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entretien introuvable"));
        if (task.getEffectueLe() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet entretien est deja termine");
        if (!Objects.equals(version, task.getVersion())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet entretien a ete modifie. Actualisez la fiche.");
        return task;
    }

    private CamionEntretienDTO response(CamionEntretien task, Camion truck) {
        return CamionEntretienDTO.from(task, truck.getKilometrage(), LocalDate.now());
    }
}
