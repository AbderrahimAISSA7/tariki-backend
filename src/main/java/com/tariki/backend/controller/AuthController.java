package com.tariki.backend.controller;

import com.tariki.backend.dto.auth.AuthResponse;
import com.tariki.backend.dto.auth.LoginRequest;
import com.tariki.backend.dto.auth.RegisterRequest;
import com.tariki.backend.service.AuthService;
import com.tariki.backend.dto.auth.ProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public ProfileResponse me(Principal principal) {
        return authService.profile(principal.getName());
    }
}
