package com.tariki.backend.service;

import com.tariki.backend.dto.FactureDTO;
import com.tariki.backend.mapper.FactureMapper;
import com.tariki.backend.model.Facture;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.repository.FactureRepository;
import com.tariki.backend.repository.LivraisonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FactureService {
    private final FactureRepository repository;
    private final FactureMapper mapper;
    private final LivraisonRepository livraisonRepository;

    public FactureService(FactureRepository repository, FactureMapper mapper, LivraisonRepository livraisonRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.livraisonRepository = livraisonRepository;
    }

    public List<FactureDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    public FactureDTO findById(Long id) {
        return repository.findById(id).map(mapper::toDTO).orElse(null);
    }

    public FactureDTO save(FactureDTO dto) {
        Livraison livraison = dto.getLivraisonId() != null ? livraisonRepository.findById(dto.getLivraisonId()).orElse(null) : null;
        Facture saved = repository.save(mapper.toEntity(dto, livraison));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
