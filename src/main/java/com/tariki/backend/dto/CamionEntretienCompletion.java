package com.tariki.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CamionEntretienCompletion(@NotNull @PastOrPresent LocalDate effectueLe,
        @NotNull @Min(0) @Max(10000000) Long effectueKm,
        @Min(0) @Max(100) Integer scorePneus, @NotNull Long version) { }
