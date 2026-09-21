package com.tariki.backend.mapper;

import com.tariki.backend.dto.EntrepriseDTO;
import com.tariki.backend.model.Entreprise;
import org.springframework.stereotype.Component;

@Component
public class EntrepriseMapper {
    public EntrepriseDTO toDTO(Entreprise entreprise) {
        EntrepriseDTO dto = new EntrepriseDTO();
        dto.setId(entreprise.getId());
        dto.setNom(entreprise.getNom());
        dto.setAdresse(entreprise.getAdresse());
        dto.setEmail(entreprise.getEmail());
        dto.setTelephone(entreprise.getTelephone());
        dto.setIce(entreprise.getIce());
        dto.setIdentifiantFiscal(entreprise.getIdentifiantFiscal());
        dto.setRegistreCommerce(entreprise.getRegistreCommerce());
        dto.setLogo(entreprise.getLogoPng() == null ? null : "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(entreprise.getLogoPng()));
        return dto;
    }
    public Entreprise toEntity(EntrepriseDTO dto) {
        return Entreprise.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .adresse(dto.getAdresse())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .build();
    }
}
