package com.tariki.backend.dto.auth;

import com.tariki.backend.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String telephone,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull User.Role role
) {
}
