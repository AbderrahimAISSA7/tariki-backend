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
        Entreprise entity = dto.getId() == null ? new Entreprise() : repository.findById(dto.getId()).orElseThrow();
        entity.setNom(dto.getNom()); entity.setAdresse(dto.getAdresse()); entity.setEmail(dto.getEmail()); entity.setTelephone(dto.getTelephone());
        Entreprise saved = repository.save(entity);
        return mapper.toDTO(saved);
    }

    public void delete(Long id) {
        scope.requireVisible(findById(id) != null);
        repository.deleteById(id);
    }

    public EntrepriseDTO billing(Long id, com.tariki.backend.dto.DeliveryWorkflowDTO.CompanyBilling request) {
        Entreprise e=repository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        scope.requireCompany(e);
        e.setNom(request.nom().trim()); e.setAdresse(request.adresse().trim()); e.setEmail(request.email().trim()); e.setTelephone(request.telephone().trim());
        e.setIce(request.ice()); e.setIdentifiantFiscal(request.identifiantFiscal()); e.setRegistreCommerce(request.registreCommerce());
        if (request.removeLogo()) e.setLogoPng(null);
        else if (request.logo()!=null) e.setLogoPng(DocumentImages.decode(request.logo(),false));
        return mapper.toDTO(repository.save(e));
    }
}
