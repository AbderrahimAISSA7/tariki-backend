package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String numero;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private java.time.Instant emiseAt;
    private BigDecimal tauxTVA;
    @Column(length = 16000)
    private String instantane;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(columnDefinition = "bytea")
    private byte[] pdf;
    @OneToOne
    private Livraison livraison;
}
