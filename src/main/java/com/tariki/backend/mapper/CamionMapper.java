package com.tariki.backend.mapper;

import com.tariki.backend.dto.CamionDTO;
import com.tariki.backend.model.Camion;
import org.springframework.stereotype.Component;

@Component
public class CamionMapper {
    public CamionDTO toDTO(Camion camion) {
        CamionDTO dto = new CamionDTO();
        dto.setId(camion.getId());
        dto.setImmatriculation(camion.getImmatriculation());
        dto.setMarque(camion.getMarque());
        dto.setModele(camion.getModele());
        dto.setCapacite(camion.getCapacite());
        return dto;
    }
    public Camion toEntity(CamionDTO dto) {
        return Camion.builder()
                .id(dto.getId())
                .immatriculation(dto.getImmatriculation())
                .marque(dto.getMarque())
                .modele(dto.getModele())
                .capacite(dto.getCapacite())
                .build();
    }
}
