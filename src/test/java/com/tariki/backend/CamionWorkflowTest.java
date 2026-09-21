package com.tariki.backend;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
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
@AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
class CamionWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CamionRepository trucks;
    @Autowired LivraisonRepository deliveries;
    @Autowired EntrepriseRepository companies;
    @Autowired UserRepository users;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;

    private String login(String role, String password) throws Exception {
        return "Bearer " + json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", role + ".demo@tariki.ma", "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private String company() throws Exception { return login("entreprise", "Entreprise123!"); }
    private JsonNode read(String path, String token) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode write(MockHttpServletRequestBuilder request, Object body, String token) throws Exception {
        return json.readTree(mvc.perform(request.header("Authorization", token).contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private ObjectNode create(String token) throws Exception {
        var data = new HashMap<String, Object>(Map.of("immatriculation", "TEST-TRUCK", "marque", "Renault", "modele", "T High", "capacite", 25,
                "carburant", "DIESEL", "nombreRoues", 10, "puissanceCh", 460, "kilometrage", 80000, "annee", 2022));
        data.put("numeroChassis", "CHASSIS-TEST"); data.put("scorePneus", 65); data.put("controlePneusLe", LocalDate.now().toString());
        return (ObjectNode) write(post("/api/camions"), data, token);
    }
    private String path(JsonNode truck) { return "/api/camions/" + truck.get("id").asLong(); }
    private String image(int width, int height) throws Exception {
        var bitmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = bitmap.createGraphics(); graphics.setColor(java.awt.Color.ORANGE); graphics.fillRect(0, 0, width, height); graphics.dispose();
        var output = new ByteArrayOutputStream(); ImageIO.write(bitmap, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    @Test void companyCanCreateAndEditMechanicalDetailsWithVersionChecks() throws Exception {
        String token = company(); ObjectNode truck = create(token); String path = path(truck);
        assertThat(truck.get("carburant").asText()).isEqualTo("DIESEL");
        assertThat(truck.get("nombreRoues").asInt()).isEqualTo(10);
        assertThat(truck.get("scorePneus").asInt()).isEqualTo(65);
        assertThat(truck.get("photoAvailable").asBoolean()).isFalse();
        truck.put("carburant", "GAZ"); truck.put("puissanceCh", 500);
        JsonNode updated = write(put(path), truck, token);
        assertThat(updated.get("version").asLong()).isGreaterThan(truck.get("version").asLong());
        assertThat(read(path, token).get("puissanceCh").asInt()).isEqualTo(500);
        mvc.perform(put(path).header("Authorization", token).contentType("application/json").content(json.writeValueAsString(truck))).andExpect(status().isConflict());
        for (String role : List.of("chauffeur", "client")) {
            String restricted = login(role, role.equals("client") ? "Client123!" : "Chauffeur123!");
            for (String suffix : List.of("", "/photo", "/entretiens")) mvc.perform(get(path + suffix).header("Authorization", restricted)).andExpect(status().isForbidden());
            mvc.perform(put(path).header("Authorization", restricted).contentType("application/json").content(json.writeValueAsString(updated))).andExpect(status().isForbidden());
        }
    }

    @Test void photosAreValidatedPrivateReplaceableAndPreservedWhenEditingDetails() throws Exception {
        String token = company(); JsonNode truck = create(token); String path = path(truck);
        mvc.perform(get(path + "/photo").header("Authorization", token)).andExpect(status().isNotFound());
        JsonNode saved = write(put(path + "/photo"), Map.of("image", image(240, 160), "version", truck.get("version").asLong()), token);
        assertThat(saved.get("photoAvailable").asBoolean()).isTrue();
        byte[] bytes = mvc.perform(get(path + "/photo").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg")).andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(bytes)).getWidth()).isEqualTo(240);
        mvc.perform(get(path + "/photo")).andExpect(status().isUnauthorized());
        ((ObjectNode) saved).put("marque", "Volvo"); saved = write(put(path), saved, token);
        assertThat(saved.get("photoAvailable").asBoolean()).isTrue();
        for (String invalid : List.of("data:image/svg+xml;base64,PHN2Zy8+", "data:image/png;base64,aW52YWxpZA==", image(2100, 50))) {
            mvc.perform(put(path + "/photo").header("Authorization", token).contentType("application/json")
                    .content(json.writeValueAsString(Map.of("image", invalid, "version", saved.get("version").asLong())))).andExpect(status().isBadRequest());
        }
        saved = write(put(path + "/photo"), Map.of("image", image(320, 180), "version", saved.get("version").asLong()), token);
        byte[] replaced = mvc.perform(get(path + "/photo").header("Authorization", token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(replaced)).getWidth()).isEqualTo(320);
        var remove = new HashMap<String, Object>(); remove.put("image", null); remove.put("version", saved.get("version").asLong());
        assertThat(write(put(path + "/photo"), remove, token).get("photoAvailable").asBoolean()).isFalse();
        mvc.perform(get(path + "/photo").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void invalidMechanicalInputsAndLowerOdometersAreRejected() throws Exception {
        String token = company(); ObjectNode truck = create(token); String path = path(truck);
        for (Map.Entry<String, Integer> field : Map.of("nombreRoues", 0, "puissanceCh", -1, "scorePneus", 101, "kilometrage", 79000, "capacite", 0).entrySet()) {
            ObjectNode invalid = truck.deepCopy(); invalid.put(field.getKey(), field.getValue());
            mvc.perform(put(path).header("Authorization", token).contentType("application/json").content(invalid.toString())).andExpect(status().isBadRequest());
        }
        ObjectNode invalid = truck.deepCopy(); invalid.put("carburant", "ESSENCE");
        mvc.perform(put(path).header("Authorization", token).contentType("application/json").content(invalid.toString())).andExpect(status().isBadRequest());
        invalid = truck.deepCopy(); invalid.putNull("controlePneusLe");
        mvc.perform(put(path).header("Authorization", token).contentType("application/json").content(invalid.toString())).andExpect(status().isBadRequest());
        invalid = truck.deepCopy(); invalid.put("controlePneusLe", LocalDate.now().plusDays(1).toString());
        mvc.perform(put(path).header("Authorization", token).contentType("application/json").content(invalid.toString())).andExpect(status().isBadRequest());
    }

    @Test void remindersUseEitherDeadlineAndUpdateAfterMileageChanges() throws Exception {
        String token = company(); ObjectNode truck = create(token); String path = path(truck);
        String tasks = path + "/entretiens";
        JsonNode due = write(post(tasks), Map.of("type", "VIDANGE", "echeanceDate", LocalDate.now().toString(), "echeanceKm", 100000), token);
        assertThat(due.get("statut").asText()).isEqualTo("A_FAIRE");
        assertThat(write(post(tasks), Map.of("type", "PNEUS", "echeanceKm", 81000), token).get("statut").asText()).isEqualTo("PROCHE");
        assertThat(write(post(tasks), Map.of("type", "FREINS", "echeanceDate", LocalDate.now().plusDays(30).toString()), token).get("statut").asText()).isEqualTo("PROCHE");
        assertThat(write(post(tasks), Map.of("type", "REVISION", "echeanceDate", LocalDate.now().plusDays(31).toString(), "echeanceKm", 82000), token).get("statut").asText()).isEqualTo("PLANIFIE");
        assertThat(read(path, token).get("entretiensUrgents").asInt()).isEqualTo(1);
        assertThat(read(path, token).get("entretiensProches").asInt()).isEqualTo(2);
        truck.put("kilometrage", 82000); write(put(path), truck, token);
        assertThat(read(path, token).get("entretiensUrgents").asInt()).isEqualTo(3);
        mvc.perform(post(tasks).header("Authorization", token).contentType("application/json").content("{\"type\":\"PNEUS\"}")).andExpect(status().isBadRequest());
    }

    @Test void completedMaintenanceIsImmutableAndUpdatesTireScoreAndOdometer() throws Exception {
        String token = company(); ObjectNode truck = create(token); String path = path(truck); String tasks = path + "/entretiens";
        JsonNode item = write(post(tasks), Map.of("type", "PNEUS", "echeanceKm", 80000, "notes", "Controle et remplacement"), token);
        String task = tasks + "/" + item.get("id").asLong();
        var completion = Map.of("effectueLe", LocalDate.now().toString(), "effectueKm", 80500, "scorePneus", 95, "version", item.get("version").asLong());
        JsonNode completed = write(post(task + "/terminer"), completion, token);
        assertThat(completed.get("statut").asText()).isEqualTo("TERMINE");
        JsonNode refreshed = read(path, token);
        assertThat(refreshed.get("scorePneus").asInt()).isEqualTo(95);
        assertThat(refreshed.get("kilometrage").asLong()).isEqualTo(80500);
        assertThat(refreshed.get("entretiensUrgents").asInt()).isZero();
        mvc.perform(post(task + "/terminer").header("Authorization", token).contentType("application/json").content(json.writeValueAsString(completion))).andExpect(status().isConflict());
        mvc.perform(delete(task).param("version", completed.get("version").asText()).header("Authorization", token)).andExpect(status().isConflict());
        mvc.perform(put(task).header("Authorization", token).contentType("application/json").content(json.writeValueAsString(Map.of("type", "REVISION", "echeanceKm", 90000, "version", completed.get("version").asLong())))).andExpect(status().isConflict());
        mvc.perform(delete(path).header("Authorization", token)).andExpect(status().isConflict());
    }

    @Test void maintenanceCanBeRescheduledAndDeletedButNotWithAStaleVersion() throws Exception {
        String token = company(); ObjectNode truck = create(token); String tasks = path(truck) + "/entretiens";
        JsonNode item = write(post(tasks), Map.of("type", "VIDANGE", "echeanceKm", 81000), token);
        String task = tasks + "/" + item.get("id").asLong();
        var update = Map.of("type", "VIDANGE", "echeanceKm", 85000, "version", item.get("version").asLong());
        JsonNode updated = write(put(task), update, token);
        assertThat(updated.get("statut").asText()).isEqualTo("PLANIFIE");
        mvc.perform(put(task).header("Authorization", token).contentType("application/json").content(json.writeValueAsString(update))).andExpect(status().isConflict());
        mvc.perform(delete(task).param("version", item.get("version").asText()).header("Authorization", token)).andExpect(status().isConflict());
        mvc.perform(delete(task).param("version", updated.get("version").asText()).header("Authorization", token)).andExpect(status().isNoContent());
        assertThat(read(tasks, token)).isEmpty();
        truck.put("carburant", "ELECTRIQUE"); write(put(path(truck)), truck, token);
        mvc.perform(post(tasks).header("Authorization", token).contentType("application/json").content("{\"type\":\"VIDANGE\",\"echeanceKm\":90000}")).andExpect(status().isBadRequest());
    }

    @Test void anotherCompanyCannotReadOrChangePhotosOrMaintenanceAndChildIdsAreScoped() throws Exception {
        String token = company(); ObjectNode truck = create(token); String path = path(truck);
        JsonNode item = write(post(path + "/entretiens"), Map.of("type", "REVISION", "echeanceKm", 90000), token);
        Entreprise other = companies.save(Entreprise.builder().nom("Autre transporteur").build());
        users.save(ResponsableEntreprise.builder().entreprise(other).role(User.Role.ENTREPRISE)
                .username("othertruck.demo@tariki.ma").password(passwords.encode("TruckPass123!")).build());
        String foreign = login("othertruck", "TruckPass123!");
        for (String suffix : List.of("", "/photo", "/entretiens")) mvc.perform(get(path + suffix).header("Authorization", foreign)).andExpect(status().isNotFound());
        mvc.perform(put(path).header("Authorization", foreign).contentType("application/json").content(truck.toString())).andExpect(status().isNotFound());
        mvc.perform(put(path + "/photo").header("Authorization", foreign).contentType("application/json").content("{\"version\":0,\"image\":null}")).andExpect(status().isNotFound());
        mvc.perform(post(path + "/entretiens").header("Authorization", foreign).contentType("application/json").content("{\"type\":\"REVISION\",\"echeanceKm\":90000}")).andExpect(status().isNotFound());
        JsonNode second = create(token);
        mvc.perform(delete(path(second) + "/entretiens/" + item.get("id").asLong()).param("version", item.get("version").asText()).header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test void truckCapacityCannotInvalidateAnAssignedDelivery() throws Exception {
        String token = company(); Livraison delivery = deliveries.findAll().stream().filter(d -> "OT-2026-001".equals(d.getReference())).findFirst().orElseThrow();
        String path = "/api/camions/" + delivery.getCamion().getId();
        ObjectNode truck = (ObjectNode) read(path, token); truck.put("capacite", 1);
        mvc.perform(put(path).header("Authorization", token).contentType("application/json").content(truck.toString())).andExpect(status().isConflict());
        mvc.perform(delete(path).header("Authorization", token)).andExpect(status().isConflict());
    }
}
