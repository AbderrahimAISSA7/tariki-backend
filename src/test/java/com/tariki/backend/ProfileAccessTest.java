package com.tariki.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariki.backend.config.DemoProfiles;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profiles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false", "logging.level.org.springframework.web=WARN"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class ProfileAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired EntrepriseRepository entreprises;
    @Autowired LivraisonRepository livraisons;
    @Autowired CamionRepository camions;
    @Autowired FactureRepository factures;
    @Autowired SignatureRepository signatures;
    @Autowired PasswordEncoder passwords;
    @Autowired DemoProfiles demo;

    private String login(String account, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", account + ".demo@tariki.ma", "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    private Livraison delivery(String reference) {
        return livraisons.findAll().stream().filter(l -> reference.equals(l.getReference())).findFirst().orElseThrow();
    }

    private JsonNode read(String path, String token) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test void companySeesItsFleetAndAllSevenDeliveries() throws Exception {
        String token = login("entreprise", "Entreprise123!");
        assertThat(read("/api/chauffeurs", token).size()).isEqualTo(3);
        assertThat(read("/api/camions", token).size()).isEqualTo(3);
        assertThat(read("/api/livraisons", token).size()).isEqualTo(7);
        assertThat(read("/api/clients", token).size()).isEqualTo(2);
        assertThat(read("/api/entreprises", token).size()).isEqualTo(1);
        assertThat(read("/api/auth/me", token).get("entrepriseNom").asText()).isEqualTo("Oriental Transport");
    }

    @Test void driverSeesOnlyHisFiveMissionsWithAllThreeStatuses() throws Exception {
        String token = login("chauffeur", "Chauffeur123!");
        JsonNode data = read("/api/livraisons", token);
        assertThat(data.size()).isEqualTo(5);
        Map<String, Long> counts = java.util.stream.StreamSupport.stream(data.spliterator(), false)
                .collect(Collectors.groupingBy(n -> n.get("statut").asText(), Collectors.counting()));
        assertThat(counts).containsEntry("EN_COURS", 1L).containsEntry("LIVREE", 2L).containsEntry("PROGRAMMEE", 2L);
        for (String path : Set.of("camions", "chauffeurs", "clients", "entreprises", "factures")) {
            mvc.perform(get("/api/" + path).header("Authorization", token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/livraisons/" + delivery("OT-2026-006").getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test void clientSeesOnlyTenTonnesOfCementAndCannotReachManagement() throws Exception {
        String token = login("client", "Client123!");
        JsonNode data = read("/api/livraisons", token);
        assertThat(data.size()).isEqualTo(1);
        assertThat(data.get(0).get("villeDepart").asText()).isEqualTo("Oujda");
        assertThat(data.get(0).get("villeArrivee").asText()).isEqualTo("Figuig");
        assertThat(data.get(0).get("poidsTonnes").asDouble()).isEqualTo(10);
        assertThat(data.get(0).get("marchandise").asText()).isEqualTo("Ciment");
        for (String path : Set.of("camions", "chauffeurs", "clients", "entreprises")) {
            mvc.perform(get("/api/" + path).header("Authorization", token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/livraisons/" + delivery("OT-2026-002").getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/livraisons/" + data.get(0).get("id").asLong() + "/statut")
                .header("Authorization", token).contentType("application/json").content("{\"statut\":\"LIVREE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/livraisons").header("Authorization", token).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void secondCompanyCannotReadOrModifyAnotherCompany() throws Exception {
        Entreprise other = entreprises.save(Entreprise.builder().nom("Autre transporteur").build());
        users.save(ResponsableEntreprise.builder().entreprise(other).role(User.Role.ENTREPRISE)
                .username("other.demo@tariki.ma").password(passwords.encode("OtherPass123!")).build());
        String token = login("other", "OtherPass123!");
        assertThat(read("/api/livraisons", token)).isEmpty();
        assertThat(read("/api/camions", token)).isEmpty();
        assertThat(read("/api/chauffeurs", token)).isEmpty();
        assertThat(read("/api/clients", token)).isEmpty();
        Livraison foreign = delivery("OT-2026-001");
        mvc.perform(get("/api/livraisons/" + foreign.getId()).header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/camions/" + foreign.getCamion().getId()).header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(put("/api/chauffeurs/" + foreign.getChauffeur().getId()).header("Authorization", token)
                .contentType("application/json").content("{}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/livraisons").header("Authorization", token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("reference", "FOREIGN", "dateLivraison", "2026-10-01",
                        "villeDepart", "Oujda", "villeArrivee", "Figuig", "marchandise", "Ciment", "poidsTonnes", 10,
                        "chauffeurId", foreign.getChauffeur().getId(), "camionId", foreign.getCamion().getId(),
                        "clientId", foreign.getClient().getId())))).andExpect(status().isBadRequest());
    }

    @Test void driverCanFinishThenStartAndClientSeesUpdatedStatus() throws Exception {
        String token = login("chauffeur", "Chauffeur123!");
        Long active = delivery("OT-2026-001").getId();
        Long planned = delivery("OT-2026-004").getId();
        mvc.perform(patch("/api/livraisons/" + planned + "/statut").header("Authorization", token)
                .contentType("application/json").content("{\"statut\":\"EN_COURS\"}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/livraisons/" + active + "/statut").header("Authorization", token)
                .contentType("application/json").content("{\"statut\":\"LIVREE\"}")).andExpect(status().isOk());
        assertThat(read("/api/livraisons/" + active, login("client", "Client123!")).get("statut").asText()).isEqualTo("LIVREE");
        mvc.perform(patch("/api/livraisons/" + planned + "/statut").header("Authorization", token)
                .contentType("application/json").content("{\"statut\":\"EN_COURS\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/livraisons/" + active + "/statut").header("Authorization", token)
                .contentType("application/json").content("{\"statut\":\"PROGRAMMEE\"}")).andExpect(status().isConflict());
    }

    @Test void invoicesAndSignaturesFollowDeliveryOwnership() throws Exception {
        Livraison own = delivery("OT-2026-001");
        Livraison other = delivery("OT-2026-002");
        factures.save(Facture.builder().livraison(own).numero("OWN").build());
        Facture hidden = factures.save(Facture.builder().livraison(other).numero("HIDDEN").build());
        signatures.save(Signature.builder().livraison(own).signataire("Client").build());
        Signature hiddenSignature = signatures.save(Signature.builder().livraison(other).signataire("Other").build());
        String token = login("client", "Client123!");
        assertThat(read("/api/factures", token).size()).isEqualTo(1);
        assertThat(read("/api/signatures", token).size()).isEqualTo(1);
        mvc.perform(get("/api/factures/" + hidden.getId()).header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/signatures/" + hiddenSignature.getId()).header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void companyCanScheduleDeliveryButCannotExceedTruckCapacity() throws Exception {
        Livraison assigned = delivery("OT-2026-001");
        String token = login("entreprise", "Entreprise123!");
        var payload = new java.util.HashMap<String, Object>(Map.of("reference", "TEST-NEW",
                "dateLivraison", "2026-10-01", "villeDepart", "Oujda", "villeArrivee", "Figuig",
                "marchandise", "Ciment", "poidsTonnes", 10, "chauffeurId", assigned.getChauffeur().getId(),
                "camionId", assigned.getCamion().getId(), "clientId", assigned.getClient().getId()));
        payload.put("statut", "LIVREE");
        payload.put("entrepriseId", -1);
        JsonNode created = json.readTree(mvc.perform(post("/api/livraisons").header("Authorization", token)
                        .contentType("application/json").content(json.writeValueAsString(payload)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(created.get("statut").asText()).isEqualTo("PROGRAMMEE");
        assertThat(created.get("entrepriseId").asLong()).isEqualTo(assigned.getEntreprise().getId());
        String path = "/api/livraisons/" + created.get("id").asLong();
        assertThat(read(path, login("chauffeur", "Chauffeur123!")).get("reference").asText()).isEqualTo("TEST-NEW");
        assertThat(read(path, login("client", "Client123!")).get("poidsTonnes").asDouble()).isEqualTo(10);
        payload.put("poidsTonnes", assigned.getCamion().getCapacite() + 1);
        mvc.perform(post("/api/livraisons").header("Authorization", token).contentType("application/json")
                .content(json.writeValueAsString(payload))).andExpect(status().isBadRequest());
        mvc.perform(delete(path).header("Authorization", token)).andExpect(status().isNoContent());
        assertThat(livraisons.count()).isEqualTo(7);
    }

    @Test void demoInitializationPreservesStateAndDoesNotDuplicate() {
        long userCount = users.count();
        Livraison changed = delivery("OT-2026-001");
        changed.setStatut("LIVREE");
        livraisons.saveAndFlush(changed);
        demo.run();
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(livraisons.count()).isEqualTo(7);
        assertThat(camions.count()).isEqualTo(3);
        assertThat(delivery("OT-2026-001").getStatut()).isEqualTo("LIVREE");
    }

    @Test void missingAndInvalidTokensAreRejectedAndCorsStillWorks() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/livraisons").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(options("/api/livraisons/1/statut").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH").header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
