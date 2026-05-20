package com.tariki.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SignatureDTO {
    private Long id;
    private String signataire;
    private LocalDateTime dateSignature;
    private String type;
    private Long livraisonId;
}
