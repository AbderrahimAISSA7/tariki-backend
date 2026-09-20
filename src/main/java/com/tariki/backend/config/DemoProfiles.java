package com.tariki.backend.config;

import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Profile("demo")
public class DemoProfiles implements CommandLineRunner {
    private final EntrepriseRepository entreprises;
    private final UserRepository users;
    private final CamionRepository camions;
    private final LivraisonRepository livraisons;
    private final PasswordEncoder passwords;

    public DemoProfiles(EntrepriseRepository entreprises, UserRepository users, CamionRepository camions,
                        LivraisonRepository livraisons, PasswordEncoder passwords) {
        this.entreprises = entreprises;
        this.users = users;
        this.camions = camions;
        this.livraisons = livraisons;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Entreprise entreprise = entreprises.findAll().stream()
                .filter(e -> "entreprise.demo@tariki.ma".equals(e.getEmail())).findFirst()
                .orElseGet(() -> entreprises.save(Entreprise.builder().nom("Oriental Transport")
                        .email("entreprise.demo@tariki.ma").telephone("+212 536 70 20 10")
                        .adresse("Zone industrielle, Oujda").build()));
        if (!users.existsByUsernameIgnoreCase("entreprise.demo@tariki.ma")) {
            users.save(ResponsableEntreprise.builder().username("entreprise.demo@tariki.ma")
                    .password(passwords.encode("Entreprise123!")).role(User.Role.ENTREPRISE).entreprise(entreprise)
                    .prenom("Salma").nom("Bennani").build());
        }
        Chauffeur chauffeur = driver(entreprise, "chauffeur.demo@tariki.ma", "Youssef", "El Amrani", "Chauffeur123!");
        Chauffeur second = driver(entreprise, "chauffeur.2.demo@tariki.ma", "Karim", "Idrissi", UUID.randomUUID().toString());
        driver(entreprise, "chauffeur.3.demo@tariki.ma", "Rachid", "Alaoui", UUID.randomUUID().toString());
        Client client = client(entreprise, "client.demo@tariki.ma", "Figuig Materiaux", "Amine", "Figuig", "Client123!");
        Client other = client(entreprise, "negoce.demo@tariki.ma", "Oriental Negoce", "Nadia", "Berkane", UUID.randomUUID().toString());
        Camion truck = truck(entreprise, "34567-A-48", "Renault", "T High", 25);
        Camion secondTruck = truck(entreprise, "67890-B-48", "Volvo", "FH", 30);
        truck(entreprise, "12345-C-48", "Mercedes", "Actros", 20);

        delivery("OT-2026-001", entreprise, chauffeur, truck, client, "Oujda", "Figuig", "Ciment", "10", "EN_COURS", 0, "Bouarfa");
        delivery("OT-2026-002", entreprise, chauffeur, truck, other, "Oujda", "Berkane", "Briques", "18", "LIVREE", -4, "Berkane");
        delivery("OT-2026-003", entreprise, chauffeur, truck, other, "Nador", "Oujda", "Acier", "12", "LIVREE", -2, "Oujda");
        delivery("OT-2026-004", entreprise, chauffeur, truck, other, "Oujda", "Taourirt", "Gravier", "20", "PROGRAMMEE", 2, "Oujda");
        delivery("OT-2026-005", entreprise, chauffeur, truck, other, "Berkane", "Nador", "Palettes", "8", "PROGRAMMEE", 4, "Berkane");
        delivery("OT-2026-006", entreprise, second, secondTruck, other, "Nador", "Berkane", "Materiaux", "15", "EN_COURS", 0, "Zaio");
        delivery("OT-2026-007", entreprise, second, secondTruck, other, "Taourirt", "Oujda", "Sable", "22", "LIVREE", -1, "Oujda");
    }

    private Chauffeur driver(Entreprise entreprise, String email, String prenom, String nom, String password) {
        return (Chauffeur) users.findByUsernameIgnoreCase(email).orElseGet(() -> users.save(Chauffeur.builder()
                .username(email).password(passwords.encode(password)).role(User.Role.CHAUFFEUR).entreprise(entreprise)
                .email(email).prenom(prenom).nom(nom).telephone("+212 600 00 00 10").build()));
    }

    private Client client(Entreprise entreprise, String email, String nom, String prenom, String ville, String password) {
        return (Client) users.findByUsernameIgnoreCase(email).orElseGet(() -> users.save(Client.builder()
                .username(email).password(passwords.encode(password)).role(User.Role.CLIENT).entreprise(entreprise)
                .email(email).prenom(prenom).nom(nom).adresse(ville).telephone("+212 600 00 00 20").build()));
    }

    private Camion truck(Entreprise entreprise, String plate, String marque, String modele, int capacity) {
        return camions.findAll().stream().filter(c -> plate.equals(c.getImmatriculation())).findFirst()
                .orElseGet(() -> camions.save(Camion.builder().entreprise(entreprise).immatriculation(plate)
                        .marque(marque).modele(modele).capacite(capacity).build()));
    }

    private void delivery(String reference, Entreprise entreprise, Chauffeur chauffeur, Camion camion, Client client,
                          String depart, String arrivee, String goods, String tonnes, String statut, int days, String position) {
        if (livraisons.findAll().stream().anyMatch(l -> reference.equals(l.getReference()))) return;
        livraisons.save(Livraison.builder().reference(reference).entreprise(entreprise).chauffeur(chauffeur)
                .camion(camion).client(client).villeDepart(depart).villeArrivee(arrivee).marchandise(goods)
                .poidsTonnes(new BigDecimal(tonnes)).statut(statut).dateLivraison(LocalDate.now().plusDays(days))
                .dernierePosition(position).miseAJour(LocalDateTime.now().plusDays(Math.min(days, 0))).build());
    }
}
