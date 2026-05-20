package com.tariki.backend.mapper;

import com.tariki.backend.dto.SignatureDTO;
import com.tariki.backend.model.Signature;
import com.tariki.backend.model.Livraison;
import org.springframework.stereotype.Component;

@Component
public class SignatureMapper {
    public SignatureDTO toDTO(Signature signature) {
        SignatureDTO dto = new SignatureDTO();
        dto.setId(signature.getId());
        dto.setSignataire(signature.getSignataire());
        dto.setDateSignature(signature.getDateSignature());
        dto.setType(signature.getType());
        dto.setLivraisonId(signature.getLivraison() != null ? signature.getLivraison().getId() : null);
        return dto;
    }
    public Signature toEntity(SignatureDTO dto, Livraison livraison) {
        return Signature.builder()
                .id(dto.getId())
                .signataire(dto.getSignataire())
                .dateSignature(dto.getDateSignature())
                .type(dto.getType())
                .livraison(livraison)
                .build();
    }
}
