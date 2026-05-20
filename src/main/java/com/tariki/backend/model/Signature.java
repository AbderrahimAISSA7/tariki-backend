package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String signataire;
    private LocalDateTime dateSignature;
    private String type; // CHAUFFEUR ou CLIENT
    @ManyToOne
    private Livraison livraison;
}
