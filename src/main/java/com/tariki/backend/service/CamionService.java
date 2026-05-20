package com.tariki.backend.service;

import com.tariki.backend.dto.CamionDTO;
import com.tariki.backend.mapper.CamionMapper;
import com.tariki.backend.model.Camion;
import com.tariki.backend.repository.CamionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CamionService {
    private final CamionRepository repository;
    private final CamionMapper mapper;

    public CamionService(CamionRepository repository, CamionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<CamionDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    public CamionDTO findById(Long id) {
        return repository.findById(id).map(mapper::toDTO).orElse(null);
    }

    public CamionDTO save(CamionDTO dto) {
        Camion saved = repository.save(mapper.toEntity(dto));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
