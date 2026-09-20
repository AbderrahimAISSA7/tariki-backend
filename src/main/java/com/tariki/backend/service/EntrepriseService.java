package com.tariki.backend.service;

import com.tariki.backend.dto.EntrepriseDTO;
import com.tariki.backend.mapper.EntrepriseMapper;
import com.tariki.backend.model.Entreprise;
import com.tariki.backend.repository.EntrepriseRepository;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EntrepriseService {
    private final AccessScope scope;
    private final EntrepriseRepository repository;
    private final EntrepriseMapper mapper;

    public EntrepriseService(EntrepriseRepository repository, EntrepriseMapper mapper, AccessScope scope) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<EntrepriseDTO> findAll() {
        var viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.company(viewer, item)).map(mapper::toDTO).collect(Collectors.toList());
    }

    public EntrepriseDTO findById(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.company(viewer, item)).map(mapper::toDTO).orElse(null);
    }

    public EntrepriseDTO save(EntrepriseDTO dto) {
        if (dto.getId() != null) scope.requireVisible(findById(dto.getId()) != null);
        Entreprise saved = repository.save(mapper.toEntity(dto));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        repository.deleteById(id);
    }
}
