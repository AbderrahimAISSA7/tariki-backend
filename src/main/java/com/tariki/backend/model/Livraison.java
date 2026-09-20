package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livraison {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String reference;
    private LocalDate dateLivraison;
    private String statut;
    private String villeDepart;
    private String villeArrivee;
    private String marchandise;
    private java.math.BigDecimal poidsTonnes;
    private String dernierePosition;
    private java.time.LocalDateTime miseAJour;
    @Version
    private Long version;
    @ManyToOne
    private Entreprise entreprise;

    @ManyToOne
    private Chauffeur chauffeur;
    @ManyToOne
    private Camion camion;
    @ManyToOne
    private Client client;
}
