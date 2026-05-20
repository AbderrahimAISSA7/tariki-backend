package com.tariki.backend.dto;

import lombok.Data;

@Data
public class EntrepriseDTO {
    private Long id;
    private String nom;
    private String adresse;
    private String email;
    private String telephone;
}
