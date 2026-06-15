package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Chauffeur extends User {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    // ... autres champs et relations ...
}
