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
    public LivraisonDTO create(@RequestBody LivraisonDTO dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LivraisonDTO> update(@PathVariable Long id, @RequestBody LivraisonDTO dto) {
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
