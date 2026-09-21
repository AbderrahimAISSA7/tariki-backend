package com.tariki.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import com.tariki.backend.model.Camion.Carburant;
import java.time.LocalDate;

@Data
public class CamionDTO {
    private Long id;
    @NotBlank @Size(max = 40)
    private String immatriculation;
    @NotBlank @Size(max = 100)
    private String marque;
    @NotBlank @Size(max = 100)
    private String modele;
    @Min(1) @Max(200)
    private int capacite;
    private Carburant carburant;
    @Min(2) @Max(32)
    private Integer nombreRoues;
    @Min(1) @Max(5000)
    private Integer puissanceCh;
    @Min(1950) @Max(2100)
    private Integer annee;
    @Size(max = 40)
    private String numeroChassis;
    @Min(0) @Max(10000000)
    private Long kilometrage;
    @Min(0) @Max(100)
    private Integer scorePneus;
    @PastOrPresent
    private LocalDate controlePneusLe;
    @Size(max = 2000)
    private String notes;
    private Long version;
    private boolean photoAvailable;
    private int entretiensUrgents;
    private int entretiensProches;
}
