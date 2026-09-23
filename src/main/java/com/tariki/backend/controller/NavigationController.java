package com.tariki.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tariki.backend.dto.*;
import com.tariki.backend.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/livraisons/{id}")
public class NavigationController {
    private final LivraisonService deliveries;
    private final NavigationRoutingService routing;

    public NavigationController(LivraisonService deliveries, NavigationRoutingService routing) {
        this.deliveries = deliveries;
        this.routing = routing;
    }

    @PatchMapping("/destination")
    public LivraisonDTO destination(@PathVariable Long id, @Valid @RequestBody NavigationDTO.Destination destination) {
        return deliveries.updateDestination(id, destination);
    }

    @PostMapping("/navigation/route")
    public ResponseEntity<JsonNode> route(@PathVariable Long id, @Valid @RequestBody NavigationDTO.Origin origin) {
        var target = deliveries.navigationTarget(id);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(routing.route(origin, target));
    }
}
