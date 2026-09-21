package com.tariki.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LivraisonDTO {
    private Long id;
    private String reference;
    private LocalDate dateLivraison;
    private String statut;
    private Long chauffeurId;
    private Long camionId;
    private Long clientId;
    private Long entrepriseId;
    private String entrepriseNom;
    private String chauffeurNom;
    private String camionImmatriculation;
    private String clientNom;
    private String villeDepart;
    private String villeArrivee;
    private String marchandise;
    private java.math.BigDecimal poidsTonnes;
    private String dernierePosition;
    private java.time.LocalDateTime miseAJour;
    private Long version;
    private String serviceFacture;
    private java.math.BigDecimal prixHT;
    private java.math.BigDecimal tauxTVA;
    private java.math.BigDecimal montantTVA;
    private java.math.BigDecimal montantTTC;
    private java.time.Instant arriveeAt;
    private java.time.Instant validationAt;
    private boolean clientInvitationPending;
    @jakarta.validation.Valid
    private DeliveryWorkflowDTO.NewClient nouveauClient;
}
