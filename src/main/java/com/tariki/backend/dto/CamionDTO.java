package com.tariki.backend.dto;

import lombok.Data;

@Data
public class CamionDTO {
    private Long id;
    private String immatriculation;
    private String marque;
    private String modele;
    private int capacite;
}
