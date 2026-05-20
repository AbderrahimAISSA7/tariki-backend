package com.tariki.backend.service;

import com.tariki.backend.dto.EntrepriseDTO;
import com.tariki.backend.mapper.EntrepriseMapper;
import com.tariki.backend.model.Entreprise;
import com.tariki.backend.repository.EntrepriseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EntrepriseService {
    private final EntrepriseRepository repository;
    private final EntrepriseMapper mapper;

    public EntrepriseService(EntrepriseRepository repository, EntrepriseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<EntrepriseDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    public EntrepriseDTO findById(Long id) {
        return repository.findById(id).map(mapper::toDTO).orElse(null);
    }

    public EntrepriseDTO save(EntrepriseDTO dto) {
        Entreprise saved = repository.save(mapper.toEntity(dto));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
