package com.tariki.backend.service;

import com.tariki.backend.dto.CamionDTO;
import com.tariki.backend.mapper.CamionMapper;
import com.tariki.backend.model.Camion;
import com.tariki.backend.repository.CamionRepository;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CamionService {
    private final AccessScope scope;
    private final CamionRepository repository;
    private final CamionMapper mapper;

    public CamionService(CamionRepository repository, CamionMapper mapper, AccessScope scope) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<CamionDTO> findAll() {
        var viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.company(viewer, item.getEntreprise())).map(mapper::toDTO).collect(Collectors.toList());
    }

    public CamionDTO findById(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.company(viewer, item.getEntreprise())).map(mapper::toDTO).orElse(null);
    }

    public CamionDTO save(CamionDTO dto) {
        if (dto.getId() != null) scope.requireVisible(findById(dto.getId()) != null);
        Camion camion = mapper.toEntity(dto);
        camion.setEntreprise(dto.getId() == null ? scope.managedCompany() : repository.findById(dto.getId()).orElseThrow().getEntreprise());
        Camion saved = repository.save(camion);
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        repository.deleteById(id);
    }
}
