package com.tariki.backend.service;

import com.tariki.backend.dto.auth.AuthResponse;
import com.tariki.backend.dto.auth.LoginRequest;
import com.tariki.backend.dto.auth.RegisterRequest;
import com.tariki.backend.model.Chauffeur;
import com.tariki.backend.model.Client;
import com.tariki.backend.model.User;
import com.tariki.backend.model.ResponsableEntreprise;
import com.tariki.backend.dto.auth.ProfileResponse;
import com.tariki.backend.repository.ChauffeurRepository;
import com.tariki.backend.repository.ClientRepository;
import com.tariki.backend.repository.UserRepository;
import com.tariki.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ChauffeurRepository chauffeurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final long accessTokenExpiration;

    public AuthService(UserRepository userRepository,
                       ClientRepository clientRepository,
                       ChauffeurRepository chauffeurRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtService jwtService,
                       @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.chauffeurRepository = chauffeurRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByUsernameIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte utilise deja cet email");
        }

        User user = switch (request.role()) {
            case CLIENT -> clientRepository.save(Client.builder()
                    .username(email)
                    .password(passwordEncoder.encode(request.password()))
                    .role(User.Role.CLIENT)
                    .nom(trim(request.nom()))
                    .prenom(trim(request.prenom()))
                    .email(email)
                    .telephone(trim(request.telephone()))
                    .build());
            case CHAUFFEUR -> chauffeurRepository.save(Chauffeur.builder()
                    .username(email)
                    .password(passwordEncoder.encode(request.password()))
                    .role(User.Role.CHAUFFEUR)
                    .nom(trim(request.nom()))
                    .prenom(trim(request.prenom()))
                    .email(email)
                    .telephone(trim(request.telephone()))
                    .build());
            case ADMIN, ENTREPRISE -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce role ne peut pas etre cree publiquement");
        };

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }

        User user = userRepository.findByUsernameIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));
        return buildResponse(user);
    }

    AuthResponse buildResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateAccessToken(userDetails, user.getRole().name());

        String nom = null;
        String prenom = null;
        String email = user.getUsername();
        String telephone = null;

        if (user instanceof Chauffeur chauffeur) {
            nom = chauffeur.getNom();
            prenom = chauffeur.getPrenom();
            email = chauffeur.getEmail();
            telephone = chauffeur.getTelephone();
        } else if (user instanceof Client client) {
            nom = client.getNom();
            prenom = client.getPrenom();
            email = client.getEmail();
            telephone = client.getTelephone();
        } else if (user instanceof ResponsableEntreprise responsable) {
            nom = responsable.getNom();
            prenom = responsable.getPrenom();
            telephone = user.getEntreprise().getTelephone();
        }

        return new AuthResponse(
                accessToken,
                "Bearer",
                accessTokenExpiration / 1000,
                user.getId(),
                email,
                nom,
                prenom,
                telephone,
                user.getRole().name(),
                user.getEntreprise() != null ? user.getEntreprise().getId() : null,
                user.getEntreprise() != null ? user.getEntreprise().getNom() : null
        );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public ProfileResponse profile(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String nom = user instanceof Client c ? c.getNom() : user instanceof Chauffeur c ? c.getNom() : ((ResponsableEntreprise) user).getNom();
        String prenom = user instanceof Client c ? c.getPrenom() : user instanceof Chauffeur c ? c.getPrenom() : ((ResponsableEntreprise) user).getPrenom();
        String telephone = user instanceof Client c ? c.getTelephone() : user instanceof Chauffeur c ? c.getTelephone() : user.getEntreprise().getTelephone();
        boolean companyMember = user.getRole() == User.Role.ENTREPRISE || user.getRole() == User.Role.CHAUFFEUR
                || user.getRole() == User.Role.ADMIN;
        byte[] logo = companyMember && user.getEntreprise() != null ? user.getEntreprise().getLogoPng() : null;
        return new ProfileResponse(user.getId(), user.getUsername(), nom, prenom, telephone, user.getRole().name(),
                user.getEntreprise() != null ? user.getEntreprise().getId() : null,
                user.getEntreprise() != null ? user.getEntreprise().getNom() : null,
                user instanceof Client c ? c.getAdresse() : user.getEntreprise() != null ? user.getEntreprise().getAdresse() : null,
                logo != null && logo.length > 0 ? "data:image/png;base64," + Base64.getEncoder().encodeToString(logo) : null);
    }

    private static String trim(String value) {
        return value.trim();
    }
}
