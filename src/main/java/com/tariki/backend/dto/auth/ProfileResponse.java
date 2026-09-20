package com.tariki.backend.dto.auth;

public record ProfileResponse(Long id, String email, String nom, String prenom, String telephone,
                              String role, Long entrepriseId, String entrepriseNom, String adresse) { }
