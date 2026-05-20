package com.tariki.backend.mapper;

import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.model.Chauffeur;
import com.tariki.backend.model.Camion;
import com.tariki.backend.model.Client;
import org.springframework.stereotype.Component;

@Component
public class LivraisonMapper {
    public LivraisonDTO toDTO(Livraison livraison) {
        LivraisonDTO dto = new LivraisonDTO();
        dto.setId(livraison.getId());
        dto.setReference(livraison.getReference());
        dto.setDateLivraison(livraison.getDateLivraison());
        dto.setStatut(livraison.getStatut());
        dto.setChauffeurId(livraison.getChauffeur() != null ? livraison.getChauffeur().getId() : null);
        dto.setCamionId(livraison.getCamion() != null ? livraison.getCamion().getId() : null);
        dto.setClientId(livraison.getClient() != null ? livraison.getClient().getId() : null);
        return dto;
    }
    public Livraison toEntity(LivraisonDTO dto, Chauffeur chauffeur, Camion camion, Client client) {
        return Livraison.builder()
                .id(dto.getId())
                .reference(dto.getReference())
                .dateLivraison(dto.getDateLivraison())
                .statut(dto.getStatut())
                .chauffeur(chauffeur)
                .camion(camion)
                .client(client)
                .build();
    }
}
