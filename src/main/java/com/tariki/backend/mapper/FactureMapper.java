package com.tariki.backend.mapper;

import com.tariki.backend.dto.FactureDTO;
import com.tariki.backend.model.Facture;
import com.tariki.backend.model.Livraison;
import org.springframework.stereotype.Component;

@Component
public class FactureMapper {
    public FactureDTO toDTO(Facture facture) {
        FactureDTO dto = new FactureDTO();
        dto.setId(facture.getId());
        dto.setNumero(facture.getNumero());
        dto.setMontantHT(facture.getMontantHT());
        dto.setMontantTVA(facture.getMontantTVA());
        dto.setMontantTTC(facture.getMontantTTC());
        dto.setLivraisonId(facture.getLivraison() != null ? facture.getLivraison().getId() : null);
        return dto;
    }
    public Facture toEntity(FactureDTO dto, Livraison livraison) {
        return Facture.builder()
                .id(dto.getId())
                .numero(dto.getNumero())
                .montantHT(dto.getMontantHT())
                .montantTVA(dto.getMontantTVA())
                .montantTTC(dto.getMontantTTC())
                .livraison(livraison)
                .build();
    }
}
