package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String username;
    private String password;
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean invitationPending;
    @Enumerated(EnumType.STRING)
    private Role role;
    @ManyToOne
    private Entreprise entreprise;
    public enum Role {
        ADMIN, ENTREPRISE, CHAUFFEUR, CLIENT
    }
}
