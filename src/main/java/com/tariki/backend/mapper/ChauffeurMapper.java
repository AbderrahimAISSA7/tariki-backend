package com.tariki.backend.mapper;

import com.tariki.backend.dto.ChauffeurDTO;
import com.tariki.backend.model.Chauffeur;
import org.springframework.stereotype.Component;

@Component
public class ChauffeurMapper {
    public ChauffeurDTO toDTO(Chauffeur chauffeur) {
        ChauffeurDTO dto = new ChauffeurDTO();
        dto.setId(chauffeur.getId());
        dto.setNom(chauffeur.getNom());
        dto.setPrenom(chauffeur.getPrenom());
        dto.setEmail(chauffeur.getEmail());
        dto.setTelephone(chauffeur.getTelephone());
        return dto;
    }
    public Chauffeur toEntity(ChauffeurDTO dto) {
        return Chauffeur.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .build();
    }
}
