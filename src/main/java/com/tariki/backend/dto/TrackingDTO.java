package com.tariki.backend.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class TrackingDTO {
    private TrackingDTO() { }
    public record Start(@NotNull @Positive Long livraisonId, @NotNull UUID sessionId) { }
    public record Stop(@NotNull UUID sessionId) { }
    public record Update(@NotNull UUID sessionId, @NotNull Double latitude, @NotNull Double longitude,
                         @NotNull Double accuracy, @NotNull Instant observedAt) {
        @Override public String toString() { return "LocationUpdate[REDACTED]"; }
    }
    public record Fix(double latitude, double longitude, double accuracy, Instant observedAt, Instant receivedAt) {
        @Override public String toString() { return "LocationFix[REDACTED]"; }
    }
    public record Driver(Long chauffeurId, String chauffeurNom, Long livraisonId, String reference,
                         String villeDepart, String villeArrivee, String deliveryStatus, String status, Fix position) { }
}
