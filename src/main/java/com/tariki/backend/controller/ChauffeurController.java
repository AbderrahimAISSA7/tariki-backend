package com.tariki.backend.controller;

import com.tariki.backend.dto.ChauffeurDTO;
import com.tariki.backend.service.ChauffeurService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chauffeurs")
@Tag(name = "Chauffeurs", description = "CRUD Chauffeurs")
public class ChauffeurController {
    private final ChauffeurService service;

    public ChauffeurController(ChauffeurService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChauffeurDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChauffeurDTO> getById(@PathVariable Long id) {
        ChauffeurDTO dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ChauffeurDTO create(@RequestBody ChauffeurDTO dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChauffeurDTO> update(@PathVariable Long id, @RequestBody ChauffeurDTO dto) {
        dto.setId(id);
        ChauffeurDTO updated = service.save(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
