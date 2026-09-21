package com.tariki.backend.service;

import com.tariki.backend.dto.TrackingDTO.*;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.security.AccessScope;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class TrackingService {
    private final AccessScope scope;
    private final UserRepository users;
    private final LivraisonRepository deliveries;
    private final LivePositionRepository positions;

    public TrackingService(AccessScope scope, UserRepository users, LivraisonRepository deliveries, LivePositionRepository positions) {
        this.scope = scope;
        this.users = users;
        this.deliveries = deliveries;
        this.positions = positions;
    }

    public List<Driver> list() {
        User viewer = scope.currentUser();
        if (viewer.getRole() == User.Role.CLIENT) {
            return deliveries.findByClientId(viewer.getId()).stream().map(this::view).toList();
        }
        if (viewer.getRole() == User.Role.CHAUFFEUR) return List.of(driverView(viewer));
        List<User> drivers = viewer.getRole() == User.Role.ADMIN
                ? users.findAll().stream().filter(u -> u.getRole() == User.Role.CHAUFFEUR).toList()
                : viewer.getEntreprise() == null ? List.of() : users.findByEntrepriseIdAndRole(viewer.getEntreprise().getId(), User.Role.CHAUFFEUR);
        return drivers.stream().map(this::driverView).toList();
    }

    public Driver delivery(Long id) {
        Livraison delivery = deliveries.findById(id).orElseThrow(this::notFound);
        scope.requireDelivery(delivery);
        return view(delivery);
    }

    private Driver driverView(User driver) {
        return deliveries.findByChauffeurId(driver.getId()).stream().filter(this::active)
                .filter(d -> sameCompany(driver, d)).findFirst().map(this::view)
                .orElseGet(() -> positions.findById(driver.getId())
                        .flatMap(p -> deliveries.findById(p.getLivraisonId()).filter(d -> matches(p, d) && sameCompany(driver, d)))
                        .map(this::view).orElseGet(() -> new Driver(driver.getId(), name(driver), null, null, null, null, null, "INACTIVE", null)));
    }

    private Driver view(Livraison delivery) {
        User driver = delivery.getChauffeur();
        LivePosition position = driver == null ? null : positions.findById(driver.getId()).orElse(null);
        String status = active(delivery) ? "NOT_SHARING" : "INACTIVE";
        Fix fix = null;
        Instant now = Instant.now();
        if (position != null && matches(position, delivery) && sameCompany(driver, delivery)) {
            if (position.getObservedAt() == null) {
                if (active(delivery) && position.isSharingActive() && !position.getUpdatedAt().isBefore(now.minusSeconds(120))) status = "ACQUIRING";
            } else {
                status = !active(delivery) || !position.isSharingActive() || position.getObservedAt().isBefore(now.minusSeconds(120))
                        ? "LAST_KNOWN" : position.getObservedAt().isBefore(now.minusSeconds(30)) ? "STALE" : "LIVE";
                fix = new Fix(position.getLatitude(), position.getLongitude(), position.getAccuracy(), position.getObservedAt(), position.getUpdatedAt());
            }
        }
        return new Driver(driver == null ? null : driver.getId(), driver == null ? "Non affecte" : name(driver),
                delivery.getId(), delivery.getReference(), delivery.getVilleDepart(), delivery.getVilleArrivee(), delivery.getStatut(), status, fix);
    }

    @Transactional
    public Driver start(Start request) {
        User driver = lockedDriver();
        Livraison delivery = assignedActive(driver, request.livraisonId());
        LivePosition position = positions.findById(driver.getId()).orElseGet(LivePosition::new);
        if (request.sessionId().equals(position.getSessionId()) && matches(position, delivery)) {
            if (!position.isSharingActive()) throw stopped();
            return view(delivery);
        }
        // A previous client's position must never be reused for a new assignment.
        if (!matches(position, delivery)) {
            position.setLatitude(null);
            position.setLongitude(null);
            position.setAccuracy(null);
            position.setObservedAt(null);
            position.setUpdatedAt(Instant.now());
        }
        position.setChauffeurId(driver.getId());
        position.setLivraisonId(delivery.getId());
        position.setEntrepriseId(delivery.getEntreprise().getId());
        position.setClientId(delivery.getClient().getId());
        position.setSessionId(request.sessionId());
        position.setSharingActive(true);
        if (position.getObservedAt() == null) position.setUpdatedAt(Instant.now());
        positions.saveAndFlush(position);
        return view(delivery);
    }

    @Transactional
    public Driver update(Update update) {
        User driver = lockedDriver();
        LivePosition position = positions.findById(driver.getId()).orElseThrow(this::stopped);
        if (!position.isSharingActive() || !position.getSessionId().equals(update.sessionId())) throw stopped();
        Livraison delivery = assignedActive(driver, position.getLivraisonId());
        if (!matches(position, delivery)) throw stopped();
        Instant now = Instant.now();
        if (!Double.isFinite(update.latitude()) || update.latitude() < -90 || update.latitude() > 90
                || !Double.isFinite(update.longitude()) || update.longitude() < -180 || update.longitude() > 180
                || !Double.isFinite(update.accuracy()) || update.accuracy() <= 0 || update.accuracy() > 5000
                || update.observedAt().isBefore(now.minusSeconds(60)) || update.observedAt().isAfter(now.plusSeconds(15))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position GPS invalide, trop ancienne ou imprecise. Verifiez l'horloge et le signal GPS.");
        }
        if (position.getObservedAt() != null && !update.observedAt().isAfter(position.getObservedAt())) return view(delivery);
        position.setLatitude(update.latitude());
        position.setLongitude(update.longitude());
        position.setAccuracy(update.accuracy());
        position.setObservedAt(update.observedAt());
        position.setUpdatedAt(now);
        positions.save(position);
        return view(delivery);
    }

    @Transactional
    public void stop(Stop request) {
        User driver = lockedDriver();
        positions.findById(driver.getId()).filter(p -> p.getSessionId().equals(request.sessionId())).ifPresent(p -> {
            p.setSharingActive(false);
            positions.save(p);
        });
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void purgeEmptySessions() { positions.deleteEmptySessions(Instant.now().minusSeconds(300)); }

    private User lockedDriver() {
        User driver = scope.currentUser();
        if (driver.getRole() != User.Role.CHAUFFEUR) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        users.lockForTracking(driver.getId()).orElseThrow(this::notFound);
        return driver;
    }

    private Livraison assignedActive(User driver, Long id) {
        Livraison delivery = deliveries.findById(id).orElseThrow(this::notFound);
        if (delivery.getChauffeur() == null || !Objects.equals(driver.getId(), delivery.getChauffeur().getId())) throw notFound();
        if (!active(delivery) || delivery.getClient() == null || !sameCompany(driver, delivery)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le partage GPS exige une livraison en cours rattachee au chauffeur");
        }
        return delivery;
    }

    private boolean active(Livraison d) { return "EN_COURS".equals(d.getStatut()) || "DEMARRE".equals(d.getStatut()); }
    private boolean sameCompany(User driver, Livraison d) {
        return driver != null && driver.getEntreprise() != null && d.getEntreprise() != null
                && Objects.equals(driver.getEntreprise().getId(), d.getEntreprise().getId());
    }
    private boolean matches(LivePosition p, Livraison d) {
        return Objects.equals(p.getLivraisonId(), d.getId()) && d.getChauffeur() != null
                && Objects.equals(p.getChauffeurId(), d.getChauffeur().getId()) && d.getEntreprise() != null
                && Objects.equals(p.getEntrepriseId(), d.getEntreprise().getId()) && d.getClient() != null
                && Objects.equals(p.getClientId(), d.getClient().getId());
    }
    private String name(User user) { return user instanceof Chauffeur c ? c.getPrenom() + " " + c.getNom() : user.getUsername(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Suivi introuvable"); }
    private ResponseStatusException stopped() { return new ResponseStatusException(HttpStatus.CONFLICT, "Le partage GPS a ete arrete ou remplace sur un autre appareil"); }
}
