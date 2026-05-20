package com.tariki.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FactureDTO {
    private Long id;
    private String numero;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private Long livraisonId;
}
