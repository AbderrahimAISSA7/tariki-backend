package com.tariki.backend.dto;

import lombok.Data;

@Data
public class ClientDTO {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
}
