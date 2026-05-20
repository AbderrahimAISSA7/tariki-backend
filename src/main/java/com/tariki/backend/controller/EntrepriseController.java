package com.tariki.backend.controller;

import com.tariki.backend.dto.EntrepriseDTO;
import com.tariki.backend.service.EntrepriseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@Tag(name = "Entreprises", description = "CRUD Entreprises")
public class EntrepriseController {
    private final EntrepriseService service;

    public EntrepriseController(EntrepriseService service) {
        this.service = service;
    }

    @GetMapping
    public List<EntrepriseDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntrepriseDTO> getById(@PathVariable Long id) {
        EntrepriseDTO dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public EntrepriseDTO create(@RequestBody EntrepriseDTO dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntrepriseDTO> update(@PathVariable Long id, @RequestBody EntrepriseDTO dto) {
        dto.setId(id);
        EntrepriseDTO updated = service.save(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
