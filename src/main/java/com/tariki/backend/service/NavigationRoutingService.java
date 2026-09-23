package com.tariki.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariki.backend.dto.NavigationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

@Service
public class NavigationRoutingService {
    private final ObjectMapper json;
    private final String baseUrl;
    private final String userAgent;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final Map<Long, Instant> attempts = new HashMap<>();
    private Instant lastRequest = Instant.EPOCH;

    public NavigationRoutingService(ObjectMapper json,
            @Value("${app.navigation.osrm-url}") String baseUrl,
            @Value("${app.navigation.user-agent}") String userAgent) {
        this.json = json;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.userAgent = userAgent;
    }

    public JsonNode route(NavigationDTO.Origin origin, NavigationDTO.Target target) {
        Instant now = Instant.now();
        if (!NavigationDTO.coordinates(origin.latitude(), origin.longitude()) || origin.accuracy() == null
                || !Double.isFinite(origin.accuracy()) || origin.accuracy() <= 0 || origin.accuracy() > 100
                || origin.observedAt() == null || origin.observedAt().isBefore(now.minusSeconds(30))
                || origin.observedAt().isAfter(now.plusSeconds(15))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une position GPS recente et precise est necessaire au guidage");
        }
        reserve(target.driverId(), now);
        // Only coordinates leave Tariki. The delivery address, IDs and JWT are never sent to the routing provider.
        String path = String.format(Locale.ROOT, "/route/v1/driving/%.6f,%.6f;%.6f,%.6f?steps=true&geometries=geojson&overview=full&alternatives=false&radiuses=100;200",
                origin.longitude(), origin.latitude(), target.longitude(), target.latitude());
        try {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(12))
                    .header("User-Agent", userAgent).header("Accept", "application/json").GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service d'itineraire occupe. Reessayez dans un instant.");
            if (response.statusCode() != 200 && response.statusCode() != 400) throw unavailable();
            JsonNode body = json.readTree(response.body());
            if (body == null) throw unavailable();
            String code = body.path("code").asText();
            if (List.of("NoRoute", "NoSegment").contains(code)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Aucun trajet routier trouve. Verifiez le point de livraison et l'acces a la route.");
            }
            JsonNode route = body.path("routes").path(0);
            if (!"Ok".equals(code) || !route.path("geometry").path("coordinates").isArray()
                    || route.path("geometry").path("coordinates").size() < 2 || !route.path("legs").isArray()
                    || route.path("legs").isEmpty() || !route.path("distance").isNumber() || !route.path("duration").isNumber()) throw unavailable();
            return route;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException e) {
            throw unavailable();
        }
    }

    private synchronized void reserve(Long driverId, Instant now) {
        attempts.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(60)));
        if (now.isBefore(lastRequest.plusMillis(1100)) || now.isBefore(attempts.getOrDefault(driverId, Instant.EPOCH).plusSeconds(10))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Calcul trop frequent. Patientez quelques secondes.");
        }
        lastRequest = now;
        attempts.put(driverId, now);
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Calcul d'itineraire indisponible. Verifiez le reseau et reessayez.");
    }
}
