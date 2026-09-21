package com.tariki.backend.service;

import com.tariki.backend.dto.FactureDTO;
import com.tariki.backend.mapper.FactureMapper;
import com.tariki.backend.model.Facture;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.repository.FactureRepository;
import com.tariki.backend.repository.LivraisonRepository;
import org.springframework.stereotype.Service;
import com.tariki.backend.security.AccessScope;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FactureService {
    private final AccessScope scope;
    private final FactureRepository repository;
    private final FactureMapper mapper;
    private final LivraisonRepository livraisonRepository;

    public FactureService(FactureRepository repository, FactureMapper mapper, LivraisonRepository livraisonRepository, AccessScope scope) {
        this.scope = scope;
        this.repository = repository;
        this.mapper = mapper;
        this.livraisonRepository = livraisonRepository;
    }

    public List<FactureDTO> findAll() {
        var viewer = scope.currentUser();
        return repository.findAll().stream().filter(item -> scope.delivery(viewer, item.getLivraison())).map(mapper::toDTO).collect(Collectors.toList());
    }

    public FactureDTO findById(Long id) {
        var viewer = scope.currentUser();
        return repository.findById(id).filter(item -> scope.delivery(viewer, item.getLivraison())).map(mapper::toDTO).orElse(null);
    }

    public FactureDTO save(FactureDTO dto) {
        if (dto.getId() != null) scope.requireVisible(findById(dto.getId()) != null);
        Livraison livraison = dto.getLivraisonId() != null ? livraisonRepository.findById(dto.getLivraisonId()).orElse(null) : null;
        scope.requireDelivery(livraison);
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "Les factures sont emises uniquement apres validation de reception par le client et ne sont pas modifiables");
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Une facture emise ne peut pas etre supprimee");
    }

    public Facture document(Long id) {
        Facture facture = repository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        scope.requireDelivery(facture.getLivraison());
        if (facture.getPdf() == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Aucun PDF archive pour cette ancienne facture");
        return facture;
    }
}
