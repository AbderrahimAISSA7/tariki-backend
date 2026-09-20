package com.tariki.backend.security;

import com.tariki.backend.model.*;
import com.tariki.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
public class AccessScope {
    private final UserRepository users;

    public AccessScope(UserRepository users) { this.users = users; }

    public User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public boolean company(User user, Entreprise entreprise) {
        return user.getRole() == User.Role.ADMIN || (entreprise != null && user.getEntreprise() != null
                && user.getRole() == User.Role.ENTREPRISE
                && Objects.equals(entreprise.getId(), user.getEntreprise().getId()));
    }

    public boolean delivery(User user, Livraison livraison) {
        if (livraison == null) return false;
        return switch (user.getRole()) {
            case ADMIN, ENTREPRISE -> company(user, livraison.getEntreprise());
            case CHAUFFEUR -> livraison.getChauffeur() != null && Objects.equals(user.getId(), livraison.getChauffeur().getId());
            case CLIENT -> livraison.getClient() != null && Objects.equals(user.getId(), livraison.getClient().getId());
        };
    }

    public void requireCompany(Entreprise entreprise) {
        requireVisible(company(currentUser(), entreprise));
    }

    public void requireDelivery(Livraison livraison) {
        requireVisible(delivery(currentUser(), livraison));
    }

    public void requireVisible(boolean visible) {
        if (!visible) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable");
    }

    public Entreprise managedCompany() {
        User user = currentUser();
        if (user.getEntreprise() == null || (user.getRole() != User.Role.ENTREPRISE && user.getRole() != User.Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucune entreprise rattachee au compte");
        }
        return user.getEntreprise();
    }
}
