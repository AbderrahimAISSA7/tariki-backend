package com.tariki.backend.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long id,
        String email,
        String nom,
        String prenom,
        String telephone,
        String role,
        Long entrepriseId,
        String entrepriseNom
) {
}
