package com.tariki.backend.controller;

import com.tariki.backend.dto.FactureDTO;
import com.tariki.backend.service.FactureService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/factures")
@Tag(name = "Factures", description = "CRUD Factures")
public class FactureController {
    private final FactureService service;

    public FactureController(FactureService service) {
        this.service = service;
    }

    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var invoice = service.document(id);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .header("Content-Disposition", org.springframework.http.ContentDisposition.attachment()
                        .filename(invoice.getNumero() + ".pdf", java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(invoice.getPdf());
    }

    @GetMapping
    public List<FactureDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureDTO> getById(@PathVariable Long id) {
        FactureDTO dto = service.findById(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public FactureDTO create(@RequestBody FactureDTO dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FactureDTO> update(@PathVariable Long id, @RequestBody FactureDTO dto) {
        dto.setId(id);
        FactureDTO updated = service.save(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
