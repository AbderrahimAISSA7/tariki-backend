package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class Client extends User {
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
}
