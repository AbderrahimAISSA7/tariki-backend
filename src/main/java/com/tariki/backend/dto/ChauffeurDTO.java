package com.tariki.backend.dto;

import lombok.Data;

@Data
public class ChauffeurDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
}
