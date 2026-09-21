package com.tariki.backend.service;

import com.tariki.backend.dto.SignatureDTO;
import com.tariki.backend.mapper.SignatureMapper;
import com.tariki.backend.model.Signature;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.repository.SignatureRepository;
import com.tariki.backend.repository.LivraisonRepository;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SignatureService {
    private final AccessScope scope;
    private final SignatureRepository repository;
    private final SignatureMapper mapper;
    private final LivraisonRepository livraisonRepository;

    public SignatureService(SignatureRepository repository, SignatureMapper mapper, LivraisonRepository livraisonRepository, AccessScope scope) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
        this.livraisonRepository = livraisonRepository;
    }

    public List<SignatureDTO> findAll() {
        var viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.delivery(viewer, item.getLivraison())).map(mapper::toDTO).collect(Collectors.toList());
    }

    public SignatureDTO findById(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.delivery(viewer, item.getLivraison())).map(mapper::toDTO).orElse(null);
    }

    public SignatureDTO save(SignatureDTO dto) {
        if (dto.getId() != null) scope.requireVisible(findById(dto.getId()) != null);
        protect(dto.getId());
        if ("RECEPTION_CLIENT".equals(dto.getType())) throw immutable();
        Livraison livraison = dto.getLivraisonId() != null ? livraisonRepository.findById(dto.getLivraisonId()).orElse(null) : null;
        scope.requireDelivery(livraison);
        Signature saved = repository.save(mapper.toEntity(dto, livraison));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        protect(id);
        repository.deleteById(id);
    }
    private void protect(Long id) {
        if (id != null && repository.findById(id).filter(s -> s.getUtilisateurId() != null || s.getImagePng() != null || "RECEPTION_CLIENT".equals(s.getType())).isPresent()) throw immutable();
    }
    private org.springframework.web.server.ResponseStatusException immutable() {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"La preuve de reception est immuable et doit provenir du client connecte");
    }
}
