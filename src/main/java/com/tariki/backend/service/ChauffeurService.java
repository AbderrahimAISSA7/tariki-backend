package com.tariki.backend.service;

import com.tariki.backend.dto.ChauffeurDTO;
import com.tariki.backend.mapper.ChauffeurMapper;
import com.tariki.backend.model.Chauffeur;
import com.tariki.backend.exception.ResourceNotFoundException;
import com.tariki.backend.repository.ChauffeurRepository;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChauffeurService {
    private final AccessScope scope;
    private final ChauffeurRepository repository;
    private final ChauffeurMapper mapper;

    public ChauffeurService(ChauffeurRepository repository, ChauffeurMapper mapper, AccessScope scope) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<ChauffeurDTO> findAll() {
        var viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.company(viewer, item.getEntreprise())).map(mapper::toDTO).collect(Collectors.toList());
    }

    public ChauffeurDTO findById(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.company(viewer, item.getEntreprise())).map(mapper::toDTO).orElse(null);
    }

    public ChauffeurDTO save(ChauffeurDTO dto) {
        if (dto.getId() != null) scope.requireVisible(findById(dto.getId()) != null);
        Chauffeur saved;
        if (dto.getId() == null) {
            Chauffeur created = mapper.toEntity(dto);
            created.setEntreprise(scope.managedCompany());
            created.setRole(com.tariki.backend.model.User.Role.CHAUFFEUR);
            saved = repository.save(created);
        } else {
            Chauffeur existing = repository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chauffeur introuvable : " + dto.getId()));
            existing.setNom(dto.getNom());
            existing.setPrenom(dto.getPrenom());
            existing.setEmail(dto.getEmail());
            existing.setTelephone(dto.getTelephone());
            saved = repository.save(existing);
        }
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        repository.deleteById(id);
    }
}
