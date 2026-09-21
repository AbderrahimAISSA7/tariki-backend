package com.tariki.backend.controller;

import com.tariki.backend.dto.TrackingDTO.*;
import com.tariki.backend.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingService tracking;
    public TrackingController(TrackingService tracking) { this.tracking = tracking; }

    @GetMapping
    public List<Driver> list() { return tracking.list(); }

    @GetMapping("/livraisons/{id}")
    public Driver delivery(@PathVariable Long id) { return tracking.delivery(id); }

    @PostMapping("/session")
    public Driver start(@Valid @RequestBody Start request) { return tracking.start(request); }

    @PutMapping("/position")
    public Driver update(@Valid @RequestBody Update request) { return tracking.update(request); }

    @DeleteMapping("/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@Valid @RequestBody Stop request) { tracking.stop(request); }
}
