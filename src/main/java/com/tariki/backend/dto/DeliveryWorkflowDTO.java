package com.tariki.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class DeliveryWorkflowDTO {
    private DeliveryWorkflowDTO() { }
    public record NewClient(@NotBlank @Size(max=100) String nom, @NotBlank @Size(max=100) String prenom,
                            @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=30) String telephone,
                            @NotBlank @Size(max=255) String adresse) { }
    public record Pricing(@NotBlank @Size(max=255) String serviceFacture,
                          @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer=10, fraction=2) BigDecimal prixHT,
                          @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3, fraction=2) BigDecimal tauxTVA,
                          @NotNull Long version) { }
    public record Receipt(@NotBlank @Size(max=200) String signataire, @NotBlank @Size(max=400000) String signature,
                          @AssertTrue boolean consentement, @NotNull Long version) {
        @Override public String toString() { return "Receipt[redacted]"; }
    }
    public record Invitation(String path, Instant expiresAt, boolean pending) { }
    public record InvitePreview(Long livraisonId, String reference, String entrepriseNom, String email,
                                String nom, String prenom, String telephone, String adresse, boolean pending) { }
    public record Activation(@NotBlank @Size(max=100) String nom, @NotBlank @Size(max=100) String prenom,
                             @NotBlank @Size(max=30) String telephone, @NotBlank @Size(max=255) String adresse,
                             @NotBlank @Size(min=8, max=72) String password) {
        @Override public String toString() { return "Activation[redacted]"; }
    }
    public record Documents(Long factureId, String numero, boolean pdfAvailable, String signataire, Instant validationAt) { }
    public record CompanyBilling(@NotBlank @Size(max=200) String nom, @NotBlank @Size(max=255) String adresse,
                                 @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=30) String telephone,
                                 @Size(max=30) String ice, @Size(max=30) String identifiantFiscal,
                                 @Size(max=50) String registreCommerce, @Size(max=1500000) String logo,
                                 boolean removeLogo) { }
}
