package com.tariki.backend.dto;

import com.tariki.backend.model.CamionEntretien.Type;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CamionEntretienRequest(@NotNull Type type, LocalDate echeanceDate,
        @Min(0) @Max(10000000) Long echeanceKm, @Size(max = 2000) String notes, Long version) { }
