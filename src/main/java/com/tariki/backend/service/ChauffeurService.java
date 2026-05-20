package com.tariki.backend.service;

import com.tariki.backend.dto.ChauffeurDTO;
import com.tariki.backend.mapper.ChauffeurMapper;
import com.tariki.backend.model.Chauffeur;
import com.tariki.backend.repository.ChauffeurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChauffeurService {
    private final ChauffeurRepository repository;
    private final ChauffeurMapper mapper;

    public ChauffeurService(ChauffeurRepository repository, ChauffeurMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<ChauffeurDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    public ChauffeurDTO findById(Long id) {
        return repository.findById(id).map(mapper::toDTO).orElse(null);
    }

    public ChauffeurDTO save(ChauffeurDTO dto) {
        Chauffeur saved = repository.save(mapper.toEntity(dto));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
