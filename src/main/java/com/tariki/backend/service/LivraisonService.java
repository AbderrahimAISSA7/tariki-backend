package com.tariki.backend.service;

import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.mapper.LivraisonMapper;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.model.Chauffeur;
import com.tariki.backend.model.Camion;
import com.tariki.backend.model.Client;
import com.tariki.backend.repository.LivraisonRepository;
import com.tariki.backend.repository.ChauffeurRepository;
import com.tariki.backend.repository.CamionRepository;
import com.tariki.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LivraisonService {
    private final LivraisonRepository repository;
    private final LivraisonMapper mapper;
    private final ChauffeurRepository chauffeurRepository;
    private final CamionRepository camionRepository;
    private final ClientRepository clientRepository;

    public LivraisonService(LivraisonRepository repository, LivraisonMapper mapper,
                           ChauffeurRepository chauffeurRepository, CamionRepository camionRepository, ClientRepository clientRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.chauffeurRepository = chauffeurRepository;
        this.camionRepository = camionRepository;
        this.clientRepository = clientRepository;
    }

    public List<LivraisonDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    public LivraisonDTO findById(Long id) {
        return repository.findById(id).map(mapper::toDTO).orElse(null);
    }

    public LivraisonDTO save(LivraisonDTO dto) {
        Chauffeur chauffeur = dto.getChauffeurId() != null ? chauffeurRepository.findById(dto.getChauffeurId()).orElse(null) : null;
        Camion camion = dto.getCamionId() != null ? camionRepository.findById(dto.getCamionId()).orElse(null) : null;
        Client client = dto.getClientId() != null ? clientRepository.findById(dto.getClientId()).orElse(null) : null;
        Livraison saved = repository.save(mapper.toEntity(dto, chauffeur, camion, client));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
