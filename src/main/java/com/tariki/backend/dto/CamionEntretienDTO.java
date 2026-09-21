package com.tariki.backend.dto;

import com.tariki.backend.model.CamionEntretien;
import java.time.LocalDate;

public record CamionEntretienDTO(Long id, CamionEntretien.Type type, LocalDate echeanceDate, Long echeanceKm,
        String notes, LocalDate effectueLe, Long effectueKm, Integer scorePneus, Long version, String statut) {
    public static CamionEntretienDTO from(CamionEntretien item, Long kilometrage, LocalDate today) {
        String status = "PLANIFIE";
        if (item.getEffectueLe() != null) status = "TERMINE";
        else if ((item.getEcheanceDate() != null && !item.getEcheanceDate().isAfter(today))
                || (item.getEcheanceKm() != null && kilometrage != null && item.getEcheanceKm() <= kilometrage)) status = "A_FAIRE";
        else if ((item.getEcheanceDate() != null && !item.getEcheanceDate().isAfter(today.plusDays(30)))
                || (item.getEcheanceKm() != null && kilometrage != null && item.getEcheanceKm() <= kilometrage + 1000)) status = "PROCHE";
        return new CamionEntretienDTO(item.getId(), item.getType(), item.getEcheanceDate(), item.getEcheanceKm(),
                item.getNotes(), item.getEffectueLe(), item.getEffectueKm(), item.getScorePneus(), item.getVersion(), status);
    }
}
