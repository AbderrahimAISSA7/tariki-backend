package com.tariki.backend.controller;

import com.tariki.backend.dto.CamionDTO;
import com.tariki.backend.service.CamionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/camions")
@Tag(name = "Camions", description = "CRUD Camions")
public class CamionController {
    private final CamionService service;

    public CamionController(CamionService service) {
        this.service = service;
    }

    @GetMapping
    public List<CamionDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CamionDTO> getById(@PathVariable Long id) {
        CamionDTO dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public CamionDTO create(@Valid @RequestBody CamionDTO dto) {
        dto.setId(null);
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CamionDTO> update(@PathVariable Long id, @Valid @RequestBody CamionDTO dto) {
        dto.setId(id);
        CamionDTO updated = service.save(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options", "nosniff").body(service.photo(id));
    }

    @PutMapping("/{id}/photo")
    public CamionDTO photo(@PathVariable Long id, @Valid @RequestBody PhotoRequest request) {
        return service.photo(id, request.image(), request.version());
    }

    public record PhotoRequest(@Size(max = 1500000) String image, @NotNull Long version) { }
}
