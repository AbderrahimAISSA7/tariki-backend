package com.tariki.backend.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class NavigationDTO {
    private NavigationDTO() { }

    public record Destination(@NotBlank @Size(max = 500) String adresseLivraison,
                              @NotNull Double destinationLatitude, @NotNull Double destinationLongitude,
                              @NotNull @PositiveOrZero Long version) { }

    public record Origin(@NotNull Double latitude, @NotNull Double longitude,
                         @NotNull @Positive @Max(100) Double accuracy, @NotNull Instant observedAt) { }

    public record Target(Long driverId, double latitude, double longitude) { }

    public static boolean coordinates(Double latitude, Double longitude) {
        return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }
}
