package com.tariki.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
class TrackingAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired EntrepriseRepository entreprises;
    @Autowired LivraisonRepository deliveries;
    @Autowired LivePositionRepository positions;
    @Autowired TrackingService tracking;
    @Autowired PasswordEncoder passwords;

    private String login(String name, String password) throws Exception {
        return "Bearer " + json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", name + ".demo@tariki.ma", "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private Livraison delivery(String reference) {
        return deliveries.findAll().stream().filter(d -> reference.equals(d.getReference())).findFirst().orElseThrow();
    }
    private ResultActions start(String token, UUID session, String reference) throws Exception {
        return mvc.perform(post("/api/tracking/session").header("Authorization", token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("sessionId", session, "livraisonId", delivery(reference).getId()))));
    }
    private ResultActions fix(String token, UUID session, double lat, double lon, double accuracy, Instant observed) throws Exception {
        return mvc.perform(put("/api/tracking/position").header("Authorization", token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("sessionId", session, "latitude", lat, "longitude", lon,
                        "accuracy", accuracy, "observedAt", observed))));
    }
    private ResultActions stop(String token, UUID session) throws Exception {
        return mvc.perform(delete("/api/tracking/session").header("Authorization", token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("sessionId", session))));
    }
    private JsonNode read(String token, String path) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }
    private String activePath() { return "/api/tracking/livraisons/" + delivery("OT-2026-001").getId(); }
    private LivePosition position() { return positions.findById(delivery("OT-2026-001").getChauffeur().getId()).orElseThrow(); }

    @Test void consentSessionPublishesOnlyToRelatedCompanyAndClient() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        String client = login("client", "Client123!");
        String company = login("entreprise", "Entreprise123!");
        assertThat(read(client, activePath()).get("position").isNull()).isTrue();
        UUID session = UUID.randomUUID();
        start(driver, session, "OT-2026-001").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACQUIRING"));
        fix(driver, session, 32.532, -1.963, 12, Instant.now()).andExpect(status().isOk());
        for (String token : new String[]{driver, company, client}) {
            JsonNode data = read(token, activePath());
            assertThat(data.get("status").asText()).isEqualTo("LIVE");
            assertThat(data.get("position").get("latitude").asDouble()).isEqualTo(32.532);
            assertThat(data.has("sessionId")).isFalse();
        }
        assertThat(read(client, "/api/tracking").size()).isEqualTo(1);
        assertThat(read(company, "/api/tracking").size()).isEqualTo(3);
        assertThat(read(driver, "/api/tracking").size()).isEqualTo(1);
        mvc.perform(get("/api/tracking/livraisons/" + delivery("OT-2026-006").getId()).header("Authorization", client))
                .andExpect(status().isNotFound());
        Entreprise foreign = entreprises.save(Entreprise.builder().nom("Transport externe").build());
        users.save(ResponsableEntreprise.builder().entreprise(foreign).role(User.Role.ENTREPRISE)
                .username("foreign.demo@tariki.ma").password(passwords.encode("Other123!")).build());
        String other = login("foreign", "Other123!");
        assertThat(read(other, "/api/tracking")).isEmpty();
        mvc.perform(get(activePath()).header("Authorization", other)).andExpect(status().isNotFound());
    }

    @Test void onlyAuthenticatedDriverCanWriteHisActiveDelivery() throws Exception {
        UUID session = UUID.randomUUID();
        mvc.perform(get("/api/tracking")).andExpect(status().isUnauthorized());
        for (String token : new String[]{login("client", "Client123!"), login("entreprise", "Entreprise123!")}) {
            start(token, session, "OT-2026-001").andExpect(status().isForbidden());
            fix(token, session, 32, -2, 10, Instant.now()).andExpect(status().isForbidden());
            stop(token, session).andExpect(status().isForbidden());
        }
        String driver = login("chauffeur", "Chauffeur123!");
        start(driver, session, "OT-2026-006").andExpect(status().isNotFound());
        start(driver, session, "OT-2026-004").andExpect(status().isConflict());
        start(driver, session, "OT-2026-002").andExpect(status().isConflict());
        fix(driver, session, 32, -2, 10, Instant.now()).andExpect(status().isConflict());
        assertThat(positions.count()).isZero();
    }

    @Test void stoppedOrReplacedDeviceCannotRestoreTracking() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        start(driver, first, "OT-2026-001").andExpect(status().isOk());
        fix(driver, first, 32, -2, 10, Instant.now()).andExpect(status().isOk());
        start(driver, second, "OT-2026-001").andExpect(status().isOk()).andExpect(jsonPath("$.position.latitude").value(32));
        stop(driver, first).andExpect(status().isNoContent());
        assertThat(position().getSessionId()).isEqualTo(second);
        fix(driver, first, 32, -2, 10, Instant.now()).andExpect(status().isConflict());
        fix(driver, second, 33, -2, 10, Instant.now()).andExpect(status().isOk());
        stop(driver, second).andExpect(status().isNoContent());
        fix(driver, second, 33, -2, 10, Instant.now()).andExpect(status().isConflict());
        stop(driver, second).andExpect(status().isNoContent());
        assertThat(positions.count()).isEqualTo(1);
        assertThat(position().isSharingActive()).isFalse();
        assertThat(read(driver, activePath()).get("status").asText()).isEqualTo("LAST_KNOWN");
        start(driver, second, "OT-2026-001").andExpect(status().isConflict());
    }

    @Test void invalidFixesAreRejectedAndOlderFixesNeverOverwriteNewerOnes() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        UUID session = UUID.randomUUID();
        start(driver, session, "OT-2026-001").andExpect(status().isOk());
        for (double[] values : new double[][]{{91, -2, 10}, {-91, -2, 10}, {32, 181, 10}, {32, -181, 10},
                {32, -2, 0}, {32, -2, 5001}, {Double.NaN, -2, 10}, {32, Double.POSITIVE_INFINITY, 10}}) {
            fix(driver, session, values[0], values[1], values[2], Instant.now()).andExpect(status().isBadRequest());
        }
        fix(driver, session, 32, -2, 10, Instant.now().minusSeconds(70)).andExpect(status().isBadRequest());
        fix(driver, session, 32, -2, 10, Instant.now().plusSeconds(30)).andExpect(status().isBadRequest());
        Instant timestamp = Instant.now();
        fix(driver, session, 32, -2, 10, timestamp).andExpect(status().isOk());
        Instant received = position().getUpdatedAt();
        fix(driver, session, 34, -3, 10, timestamp.minusSeconds(1)).andExpect(status().isOk());
        assertThat(position().getLatitude()).isEqualTo(32);
        assertThat(position().getUpdatedAt()).isEqualTo(received);
        start(driver, session, "OT-2026-001").andExpect(status().isOk());
        assertThat(position().getLatitude()).isEqualTo(32);
        assertThat(positions.count()).isEqualTo(1);
    }

    @Test void stalePositionsKeepTheirCoordinatesAndOriginalDatesAfterDisconnection() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        UUID session = UUID.randomUUID();
        start(driver, session, "OT-2026-001").andExpect(status().isOk());
        fix(driver, session, 32, -2, 10, Instant.now().minusSeconds(40)).andExpect(status().isOk());
        assertThat(read(driver, activePath()).get("status").asText()).isEqualTo("STALE");
        LivePosition position = position();
        position.setObservedAt(Instant.now().minusSeconds(130));
        positions.saveAndFlush(position);
        JsonNode expired = read(driver, activePath());
        assertThat(expired.get("status").asText()).isEqualTo("LAST_KNOWN");
        assertThat(expired.get("position").get("latitude").asDouble()).isEqualTo(32);
        position.setUpdatedAt(Instant.now().minusSeconds(310));
        Instant received = position.getUpdatedAt();
        Instant observed = position.getObservedAt();
        positions.saveAndFlush(position);
        tracking.purgeEmptySessions();
        assertThat(positions.count()).isEqualTo(1);
        stop(driver, session).andExpect(status().isNoContent());
        assertThat(position().getUpdatedAt()).isEqualTo(received);
        assertThat(position().getObservedAt()).isEqualTo(observed);
        JsonNode last = read(login("client", "Client123!"), activePath());
        assertThat(Instant.parse(last.get("position").get("observedAt").asText())).isEqualTo(observed);
        assertThat(Instant.parse(last.get("position").get("receivedAt").asText())).isEqualTo(received);
    }

    @Test void reassignmentDoesNotExposePreviousClientsPosition() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        UUID session = UUID.randomUUID();
        start(driver, session, "OT-2026-001").andExpect(status().isOk());
        fix(driver, session, 32, -2, 10, Instant.now()).andExpect(status().isOk());
        Livraison active = delivery("OT-2026-001");
        Client original = active.getClient();
        Client replacement = (Client) users.findAll().stream().filter(u -> u.getRole() == User.Role.CLIENT && !u.getId().equals(original.getId())).findFirst().orElseThrow();
        active.setClient(replacement);
        deliveries.saveAndFlush(active);
        assertThat(read(driver, activePath()).get("position").isNull()).isTrue();
        fix(driver, session, 32, -2, 10, Instant.now()).andExpect(status().isConflict());
        String originalClient = login("client", "Client123!");
        mvc.perform(get(activePath()).header("Authorization", originalClient)).andExpect(status().isNotFound());
        active.setClient(original);
        active.getChauffeur().setEntreprise(entreprises.save(Entreprise.builder().nom("Nouvelle entreprise").build()));
        users.flush();
        assertThat(read(driver, activePath()).get("position").isNull()).isTrue();
        fix(driver, session, 32, -2, 10, Instant.now()).andExpect(status().isConflict());
    }

    @Test void finishingDeliveryStopsLiveTrackingAndKeepsLastKnownPointScopedToItsDelivery() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        UUID session = UUID.randomUUID();
        start(driver, session, "OT-2026-001").andExpect(status().isOk());
        fix(driver, session, 32, -2, 10, Instant.now()).andExpect(status().isOk());
        mvc.perform(patch("/api/livraisons/" + delivery("OT-2026-001").getId() + "/statut").header("Authorization", driver)
                .contentType("application/json").content("{\"statut\":\"EN_ATTENTE_VALIDATION\"}")).andExpect(status().isOk());
        assertThat(positions.count()).isEqualTo(1);
        JsonNode ended = read(login("client", "Client123!"), activePath());
        assertThat(ended.get("status").asText()).isEqualTo("LAST_KNOWN");
        assertThat(ended.get("position").get("latitude").asDouble()).isEqualTo(32);
        assertThat(read(driver, "/api/tracking").get(0).get("status").asText()).isEqualTo("LAST_KNOWN");
        fix(driver, session, 32, -2, 10, Instant.now()).andExpect(status().isConflict());
        mvc.perform(patch("/api/livraisons/" + delivery("OT-2026-004").getId() + "/statut").header("Authorization", driver)
                .contentType("application/json").content("{\"statut\":\"EN_COURS\"}")).andExpect(status().isOk());
        UUID next = UUID.randomUUID();
        start(driver, next, "OT-2026-004").andExpect(status().isOk()).andExpect(jsonPath("$.position").doesNotExist());
        fix(driver, next, 34, -3, 10, Instant.now()).andExpect(status().isOk());
        assertThat(read(login("client", "Client123!"), activePath()).get("position").isNull()).isTrue();
    }

    @Test void onlyEmptyAbandonedSessionsArePurged() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        start(driver, UUID.randomUUID(), "OT-2026-001").andExpect(status().isOk());
        LivePosition empty = position();
        empty.setUpdatedAt(Instant.now().minusSeconds(310));
        positions.saveAndFlush(empty);
        tracking.purgeEmptySessions();
        assertThat(positions.count()).isZero();
    }

    @Test void httpsPreflightWorksButArbitraryOriginsRemainBlocked() throws Exception {
        mvc.perform(options("/api/tracking/position").header("Origin", "https://localhost:5175")
                .header("Access-Control-Request-Method", "PUT").header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://localhost:5175"));
        mvc.perform(options("/api/tracking/position").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "PUT")).andExpect(status().isForbidden());
    }
}
