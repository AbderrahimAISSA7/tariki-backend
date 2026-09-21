package com.tariki.backend.service;

import com.tariki.backend.dto.DeliveryWorkflowDTO.*;
import com.tariki.backend.dto.auth.AuthResponse;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.security.AccessScope;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class DeliveryInvitationService {
    private final LivraisonRepository deliveries;
    private final ClientRepository clients;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccessScope scope;
    private final AuthService auth;
    private final EntityManager entities;
    private final SecretKey key;

    public DeliveryInvitationService(LivraisonRepository deliveries, ClientRepository clients, UserRepository users,
                                     PasswordEncoder passwords, AccessScope scope, AuthService auth, EntityManager entities,
                                     @Value("${app.jwt.secret}") String secret) throws Exception {
        this.deliveries = deliveries; this.clients = clients; this.users = users;
        this.passwords = passwords; this.scope = scope; this.auth = auth; this.entities = entities;
        // Domain separation: invitation tokens can never serve as access tokens.
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Decoders.BASE64.decode(secret), "HmacSHA256"));
        this.key = Keys.hmacShaKeyFor(mac.doFinal("tariki.delivery-invitation.v1".getBytes(StandardCharsets.UTF_8)));
    }

    Client createClient(NewClient request, Entreprise company) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cet email est deja utilise. Selectionnez le client existant s'il appartient a votre entreprise.");
        return clients.save(Client.builder().username(email).email(email).nom(request.nom().trim())
                .prenom(request.prenom().trim()).telephone(request.telephone().trim()).adresse(request.adresse().trim())
                .entreprise(company).role(User.Role.CLIENT).invitationPending(true)
                .password(passwords.encode(UUID.randomUUID().toString())).build());
    }

    void initialize(Livraison delivery) {
        delivery.setInvitationId(UUID.randomUUID());
        delivery.setInvitationExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS));
    }

    public Invitation invitation(Long id, boolean renew) {
        deliveries.lockRow(id);
        Livraison delivery = deliveries.findById(id).orElseThrow(this::notFound);
        scope.requireDelivery(delivery);
        scope.requireCompany(delivery.getEntreprise());
        Client client = delivery.getClient();
        if (!client.isInvitationPending()) return new Invitation("/livraisons/" + id, null, false);
        if (renew || delivery.getInvitationId() == null) initialize(delivery);
        String token = Jwts.builder().subject(client.getId().toString()).id(delivery.getInvitationId().toString())
                .claim("delivery", id).claim("purpose", "delivery-invitation")
                .expiration(Date.from(delivery.getInvitationExpiresAt())).signWith(key).compact();
        return new Invitation("/register?invitation=" + token, delivery.getInvitationExpiresAt(), true);
    }

    private Livraison resolve(String token) {
        try {
            if (token.length() > 2048) throw notFound();
            Claims claims = Jwts.parser().verifyWith(key).require("purpose", "delivery-invitation")
                    .build().parseSignedClaims(token).getPayload();
            Long id = claims.get("delivery", Long.class);
            deliveries.lockRow(id);
            Livraison delivery = deliveries.findById(id).orElseThrow(this::notFound);
            if (!Objects.equals(claims.getId(), Objects.toString(delivery.getInvitationId(), ""))
                    || delivery.getClient() == null || !delivery.getClient().getId().toString().equals(claims.getSubject())
                    || delivery.getInvitationExpiresAt() == null || !delivery.getInvitationExpiresAt().isAfter(Instant.now())) throw notFound();
            return delivery;
        } catch (JwtException | IllegalArgumentException exception) {
            throw notFound();
        }
    }

    public InvitePreview preview(String token) {
        Livraison delivery = resolve(token);
        Client c = delivery.getClient();
        boolean pending = c.isInvitationPending();
        return new InvitePreview(delivery.getId(), delivery.getReference(), delivery.getEntreprise().getNom(),
                pending ? c.getEmail() : null, pending ? c.getNom() : null, pending ? c.getPrenom() : null,
                pending ? c.getTelephone() : null, pending ? c.getAdresse() : null, pending);
    }

    public AuthResponse activate(String token, Activation request) {
        Livraison delivery = resolve(token);
        Client c = delivery.getClient();
        users.lockForTracking(c.getId());
        entities.refresh(c);
        if (!c.isInvitationPending()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte deja active. Connectez-vous.");
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe trop long (72 octets maximum)");
        }
        c.setNom(request.nom().trim()); c.setPrenom(request.prenom().trim());
        c.setTelephone(request.telephone().trim()); c.setAdresse(request.adresse().trim());
        c.setPassword(passwords.encode(request.password())); c.setInvitationPending(false);
        clients.saveAndFlush(c);
        return auth.buildResponse(c);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation invalide ou expiree. Demandez un nouveau lien a l'entreprise.");
    }
}
