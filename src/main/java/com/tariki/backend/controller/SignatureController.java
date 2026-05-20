package com.tariki.backend.controller;

import com.tariki.backend.dto.SignatureDTO;
import com.tariki.backend.service.SignatureService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signatures")
@Tag(name = "Signatures", description = "CRUD Signatures")
public class SignatureController {
    private final SignatureService service;

    public SignatureController(SignatureService service) {
        this.service = service;
    }

    @GetMapping
    public List<SignatureDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SignatureDTO> getById(@PathVariable Long id) {
        SignatureDTO dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public SignatureDTO create(@RequestBody SignatureDTO dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SignatureDTO> update(@PathVariable Long id, @RequestBody SignatureDTO dto) {
        dto.setId(id);
        SignatureDTO updated = service.save(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
