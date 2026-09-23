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
    @Column(length = 500)
    private String adresseLivraison;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String marchandise;
    private java.math.BigDecimal poidsTonnes;
    private String dernierePosition;
    private java.time.LocalDateTime miseAJour;
    @Version
    private Long version;
    private String serviceFacture;
    @Column(precision = 14, scale = 2)
    private java.math.BigDecimal prixHT;
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal tauxTVA;
    private java.time.Instant arriveeAt;
    private java.time.Instant validationAt;
    private java.util.UUID invitationId;
    private java.time.Instant invitationExpiresAt;
    @ManyToOne
    private Entreprise entreprise;

    @ManyToOne
    private Chauffeur chauffeur;
    @ManyToOne
    private Camion camion;
    @ManyToOne
    private Client client;
}
