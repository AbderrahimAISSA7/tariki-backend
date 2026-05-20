package com.tariki.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LivraisonDTO {
    private Long id;
    private String reference;
    private LocalDate dateLivraison;
    private String statut;
    private Long chauffeurId;
    private Long camionId;
    private Long clientId;
}
