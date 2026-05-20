package com.tariki.backend.mapper;

import com.tariki.backend.dto.ClientDTO;
import com.tariki.backend.model.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {
    public ClientDTO toDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setNom(client.getNom());
        dto.setEmail(client.getEmail());
        dto.setTelephone(client.getTelephone());
        dto.setAdresse(client.getAdresse());
        return dto;
    }
    public Client toEntity(ClientDTO dto) {
        return Client.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .build();
    }
}
