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
        dto.setCarburant(camion.getCarburant());
        dto.setNombreRoues(camion.getNombreRoues());
        dto.setPuissanceCh(camion.getPuissanceCh());
        dto.setAnnee(camion.getAnnee());
        dto.setNumeroChassis(camion.getNumeroChassis());
        dto.setKilometrage(camion.getKilometrage());
        dto.setScorePneus(camion.getScorePneus());
        dto.setControlePneusLe(camion.getControlePneusLe());
        dto.setNotes(camion.getNotes());
        dto.setVersion(camion.getVersion());
        dto.setPhotoAvailable(camion.getPhotoJpeg() != null);
        return dto;
    }
    public void update(CamionDTO dto, Camion camion) {
        camion.setImmatriculation(dto.getImmatriculation().trim());
        camion.setMarque(dto.getMarque().trim());
        camion.setModele(dto.getModele().trim());
        camion.setCapacite(dto.getCapacite());
        camion.setCarburant(dto.getCarburant());
        camion.setNombreRoues(dto.getNombreRoues());
        camion.setPuissanceCh(dto.getPuissanceCh());
        camion.setAnnee(dto.getAnnee());
        camion.setNumeroChassis(dto.getNumeroChassis());
        camion.setKilometrage(dto.getKilometrage());
        camion.setScorePneus(dto.getScorePneus());
        camion.setControlePneusLe(dto.getControlePneusLe());
        camion.setNotes(dto.getNotes());
    }
}
