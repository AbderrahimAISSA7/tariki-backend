package com.tariki.backend.controller;

import com.tariki.backend.dto.*;
import com.tariki.backend.service.CamionEntretienService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/camions/{camionId}/entretiens")
public class CamionEntretienController {
    private final CamionEntretienService service;
    public CamionEntretienController(CamionEntretienService service) { this.service = service; }

    @GetMapping
    public List<CamionEntretienDTO> list(@PathVariable Long camionId) { return service.findAll(camionId); }

    @PostMapping
    public CamionEntretienDTO create(@PathVariable Long camionId, @Valid @RequestBody CamionEntretienRequest request) {
        return service.save(camionId, null, request);
    }

    @PutMapping("/{id}")
    public CamionEntretienDTO update(@PathVariable Long camionId, @PathVariable Long id, @Valid @RequestBody CamionEntretienRequest request) {
        return service.save(camionId, id, request);
    }

    @PostMapping("/{id}/terminer")
    public CamionEntretienDTO complete(@PathVariable Long camionId, @PathVariable Long id, @Valid @RequestBody CamionEntretienCompletion request) {
        return service.complete(camionId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long camionId, @PathVariable Long id, @RequestParam Long version) {
        service.delete(camionId, id, version);
        return ResponseEntity.noContent().build();
    }
}
