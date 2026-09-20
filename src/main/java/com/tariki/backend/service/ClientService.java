package com.tariki.backend.service;

import com.tariki.backend.dto.ClientDTO;
import com.tariki.backend.mapper.ClientMapper;
import com.tariki.backend.model.Client;
import com.tariki.backend.exception.ResourceNotFoundException;
import com.tariki.backend.repository.ClientRepository;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClientService {
    private final AccessScope scope;
    private final ClientRepository repository;
    private final ClientMapper mapper;

    public ClientService(ClientRepository repository, ClientMapper mapper, AccessScope scope) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<ClientDTO> findAll() {
        var viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.company(viewer, item.getEntreprise())).map(mapper::toDTO).collect(Collectors.toList());
    }

    public ClientDTO findById(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.company(viewer, item.getEntreprise())).map(mapper::toDTO).orElse(null);
    }

    public ClientDTO save(ClientDTO dto) {
        if (dto.getId() != null) scope.requireVisible(findById(dto.getId()) != null);
        Client saved;
        if (dto.getId() == null) {
            Client created = mapper.toEntity(dto);
            created.setEntreprise(scope.managedCompany());
            created.setRole(com.tariki.backend.model.User.Role.CLIENT);
            saved = repository.save(created);
        } else {
            Client existing = repository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + dto.getId()));
            existing.setNom(dto.getNom());
            existing.setPrenom(dto.getPrenom());
            existing.setEmail(dto.getEmail());
            existing.setTelephone(dto.getTelephone());
            existing.setAdresse(dto.getAdresse());
            saved = repository.save(existing);
        }
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        repository.deleteById(id);
    }
}
