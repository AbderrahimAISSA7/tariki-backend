package com.tariki.backend.service;

import com.tariki.backend.dto.SignatureDTO;
import com.tariki.backend.mapper.SignatureMapper;
import com.tariki.backend.model.Signature;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.repository.SignatureRepository;
import com.tariki.backend.repository.LivraisonRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SignatureService {
    private final SignatureRepository repository;
    private final SignatureMapper mapper;
    private final LivraisonRepository livraisonRepository;

    public SignatureService(SignatureRepository repository, SignatureMapper mapper, LivraisonRepository livraisonRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.livraisonRepository = livraisonRepository;
    }

    public List<SignatureDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    public SignatureDTO findById(Long id) {
        return repository.findById(id).map(mapper::toDTO).orElse(null);
    }

    public SignatureDTO save(SignatureDTO dto) {
        Livraison livraison = dto.getLivraisonId() != null ? livraisonRepository.findById(dto.getLivraisonId()).orElse(null) : null;
        Signature saved = repository.save(mapper.toEntity(dto, livraison));
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
