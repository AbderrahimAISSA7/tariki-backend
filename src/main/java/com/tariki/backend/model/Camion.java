package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Camion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String immatriculation;
    private String marque;
    private String modele;
    private int capacite;
    @Enumerated(EnumType.STRING)
    private Carburant carburant;
    private Integer nombreRoues;
    private Integer puissanceCh;
    private Integer annee;
    private String numeroChassis;
    private Long kilometrage;
    private Integer scorePneus;
    private java.time.LocalDate controlePneusLe;
    @Column(length = 2000)
    private String notes;
    @Column(columnDefinition = "bytea")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private byte[] photoJpeg;
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;
    @ManyToOne
    private Entreprise entreprise;
    public enum Carburant { DIESEL, GAZ, ELECTRIQUE }
}
