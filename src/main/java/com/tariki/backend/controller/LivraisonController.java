package com.tariki.backend.controller;

import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.service.LivraisonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livraisons")
@Tag(name = "Livraisons", description = "CRUD Livraisons")
public class LivraisonController {
    public record StatusRequest(@jakarta.validation.constraints.NotBlank String statut) { }

    @PatchMapping("/{id}/statut")
    public LivraisonDTO status(@PathVariable Long id, @jakarta.validation.Valid @RequestBody StatusRequest request) {
        return service.changeStatus(id, request.statut());
    }
    private final LivraisonService service;

    public LivraisonController(LivraisonService service) {
        this.service = service;
    }

    @GetMapping
    public List<LivraisonDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LivraisonDTO> getById(@PathVariable Long id) {
        LivraisonDTO dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public LivraisonDTO create(@jakarta.validation.Valid @RequestBody LivraisonDTO dto) {
        dto.setId(null);
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LivraisonDTO> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody LivraisonDTO dto) {
        dto.setId(id);
        LivraisonDTO updated = service.save(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
