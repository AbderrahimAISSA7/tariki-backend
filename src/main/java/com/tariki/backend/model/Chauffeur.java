package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class Chauffeur extends User {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    // ... autres champs et relations ...
}
