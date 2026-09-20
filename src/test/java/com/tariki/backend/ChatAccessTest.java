package com.tariki.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.service.ChatEvents;
import com.tariki.backend.service.ChatService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class ChatAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired LivraisonRepository deliveries;
    @Autowired EntrepriseRepository companies;
    @Autowired ChatMessageRepository messages;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager entityManager;
    @Autowired ChatEvents events;
    @Autowired javax.sql.DataSource dataSource;

    private String login(String account, String password) throws Exception {
        return "Bearer " + json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", account + ".demo@tariki.ma", "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    private User user(String name) { return users.findByUsernameIgnoreCase(name + ".demo@tariki.ma").orElseThrow(); }
    private Livraison delivery(String ref) { return deliveries.findAll().stream().filter(d -> ref.equals(d.getReference())).findFirst().orElseThrow(); }
    private JsonNode getJson(String path, String token) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }
    private String channel(String token, String kind) throws Exception {
        for (JsonNode conversation : getJson("/api/chat/conversations", token)) {
            if (conversation.get("kind").asText().equals(kind)) return conversation.get("key").asText();
        }
        throw new AssertionError("Conversation absente");
    }
    private String path(String key) { return "/api/chat/conversations/" + key; }
    private JsonNode send(String key, String token, String text, UUID requestId) throws Exception {
        return json.readTree(mvc.perform(post(path(key) + "/messages").header("Authorization", token)
                        .contentType("application/json").content(json.writeValueAsString(Map.of("content", text, "clientMessageId", requestId))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private void denied(String key, String token) throws Exception {
        mvc.perform(get(path(key) + "/messages").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(post(path(key) + "/messages").header("Authorization", token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("content", "Interdit", "clientMessageId", UUID.randomUUID())))).andExpect(status().isNotFound());
        mvc.perform(post(path(key) + "/read").header("Authorization", token).contentType("application/json").content("{\"throughId\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test void contactsAreDerivedFromActualRelationships() throws Exception {
        JsonNode company = getJson("/api/chat/conversations", login("entreprise", "Entreprise123!"));
        assertThat(company.size()).isEqualTo(3);
        for (JsonNode c : company) assertThat(c.get("kind").asText()).isEqualTo("INTERNAL");
        JsonNode driver = getJson("/api/chat/conversations", login("chauffeur", "Chauffeur123!"));
        assertThat(driver.size()).isEqualTo(6);
        JsonNode client = getJson("/api/chat/conversations", login("client", "Client123!"));
        assertThat(client.size()).isEqualTo(1);
        assertThat(client.get(0).get("reference").asText()).isEqualTo("OT-2026-001");
        assertThat(client.get(0).get("peerId").asLong()).isEqualTo(user("chauffeur").getId());
    }

    @Test void privateMessagesAreEncryptedAndInvisibleToCompanyAndUnassignedDrivers() throws Exception {
        String client = login("client", "Client123!");
        String driver = login("chauffeur", "Chauffeur123!");
        String key = channel(client, "DELIVERY");
        String text = "Bonjour, les 10 tonnes de ciment sont-elles a Bouarfa ?";
        JsonNode sent = send(key, client, text, UUID.randomUUID());
        assertThat(sent.get("senderId").asLong()).isEqualTo(user("client").getId());
        entityManager.flush();
        String stored = (String) entityManager.createNativeQuery("select encrypted_body from chat_message where id = :id")
                .setParameter("id", sent.get("id").asLong()).getSingleResult();
        assertThat(stored).startsWith("v1:").doesNotContain("Bonjour", "ciment", "Bouarfa");
        assertThat(getJson(path(key) + "/messages", driver).get("messages").get(0).get("content").asText()).isEqualTo(text);
        denied(key, login("entreprise", "Entreprise123!"));
        users.save(Chauffeur.builder().username("unassigned.demo@tariki.ma").role(User.Role.CHAUFFEUR)
                .entreprise(user("chauffeur").getEntreprise()).password(passwords.encode("OtherPass123!")).build());
        denied(key, login("unassigned", "OtherPass123!"));
    }

    @Test void internalConversationRejectsClientsAndOtherCompanies() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        String key = channel(driver, "INTERNAL");
        send(key, driver, "Je viens de charger le camion.", UUID.randomUUID());
        assertThat(getJson(path(key) + "/messages", login("entreprise", "Entreprise123!")).get("messages").size()).isEqualTo(1);
        denied(key, login("client", "Client123!"));
        Entreprise other = companies.save(Entreprise.builder().nom("Transport externe").build());
        users.save(ResponsableEntreprise.builder().username("other.demo@tariki.ma").role(User.Role.ENTREPRISE)
                .entreprise(other).password(passwords.encode("OtherPass123!")).build());
        String outsider = login("other", "OtherPass123!");
        assertThat(getJson("/api/chat/conversations", outsider)).isEmpty();
        denied(key, outsider);
    }

    @Test void reassignmentRevokesPrivateAccessAndDoesNotExposeOldHistory() throws Exception {
        String client = login("client", "Client123!");
        String oldKey = channel(client, "DELIVERY");
        send(oldKey, client, "Message prive avec le premier chauffeur", UUID.randomUUID());
        Livraison delivery = delivery("OT-2026-001");
        delivery.setChauffeur((Chauffeur) user("chauffeur.2"));
        deliveries.saveAndFlush(delivery);
        entityManager.clear();
        denied(oldKey, login("chauffeur", "Chauffeur123!"));
        denied(oldKey, client);
        String replacement = channel(client, "DELIVERY");
        assertThat(replacement).isNotEqualTo(oldKey);
        assertThat(getJson(path(replacement) + "/messages", client).get("messages")).isEmpty();
    }

    @Test void leavingCompanyRevokesInternalConversation() throws Exception {
        String driver = login("chauffeur", "Chauffeur123!");
        String company = login("entreprise", "Entreprise123!");
        String key = channel(driver, "INTERNAL");
        send(key, company, "Planning", UUID.randomUUID());
        User detached = user("chauffeur");
        detached.setEntreprise(null);
        users.saveAndFlush(detached);
        entityManager.clear();
        denied(key, driver);
        denied(key, company);
    }

    @Test void retriesDoNotDuplicateAndOnlyRecipientCanMarkRead() throws Exception {
        String client = login("client", "Client123!");
        String driver = login("chauffeur", "Chauffeur123!");
        String key = channel(client, "DELIVERY");
        UUID request = UUID.randomUUID();
        JsonNode sent = send(key, client, "Heure d'arrivee ?", request);
        assertThat(send(key, client, "Heure d'arrivee ?", request).get("id")).isEqualTo(sent.get("id"));
        assertThat(messages.count()).isEqualTo(1);
        String body = "{\"throughId\":" + sent.get("id") + "}";
        mvc.perform(post(path(key) + "/read").header("Authorization", client).contentType("application/json").content(body)).andExpect(status().isNoContent());
        assertThat(getJson(path(key) + "/messages", client).get("messages").get(0).get("readAt").isNull()).isTrue();
        JsonNode driverConversations = getJson("/api/chat/conversations", driver);
        assertThat(java.util.stream.StreamSupport.stream(driverConversations.spliterator(), false).mapToLong(n -> n.get("unread").asLong()).sum()).isEqualTo(1);
        mvc.perform(post(path(key) + "/read").header("Authorization", driver).contentType("application/json").content(body)).andExpect(status().isNoContent());
        entityManager.clear();
        assertThat(getJson(path(key) + "/messages", client).get("messages").get(0).get("readAt").isNull()).isFalse();
        mvc.perform(post(path(key) + "/messages").header("Authorization", client).contentType("application/json")
                .content(json.writeValueAsString(Map.of("content", "Autre contenu", "clientMessageId", request)))).andExpect(status().isConflict());
    }

    @Test void historyHasStablePaginationAndRejectsInvalidMessages() throws Exception {
        String client = login("client", "Client123!");
        String key = channel(client, "DELIVERY");
        for (int i = 0; i < 53; i++) send(key, client, "Message " + i, UUID.randomUUID());
        JsonNode recent = getJson(path(key) + "/messages", client);
        assertThat(recent.get("messages").size()).isEqualTo(50);
        assertThat(recent.get("hasMore").asBoolean()).isTrue();
        JsonNode older = getJson(path(key) + "/messages?before=" + recent.get("nextBefore"), client);
        assertThat(older.get("messages").size()).isEqualTo(3);
        assertThat(older.get("hasMore").asBoolean()).isFalse();
        assertThat(older.get("messages").get(2).get("id").asLong()).isLessThan(recent.get("messages").get(0).get("id").asLong());
        for (String content : List.of("   ", "a".repeat(4001))) {
            mvc.perform(post(path(key) + "/messages").header("Authorization", client).contentType("application/json")
                    .content(json.writeValueAsString(Map.of("content", content, "clientMessageId", UUID.randomUUID())))).andExpect(status().isBadRequest());
        }
        mvc.perform(get(path(key) + "/messages?before=-1").header("Authorization", client)).andExpect(status().isBadRequest());
    }

    @Test void liveEventsAreAuthenticatedAndOnlyNotifyParticipants() throws Exception {
        mvc.perform(get("/api/chat/events")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/events").param("token", login("client", "Client123!"))).andExpect(status().isUnauthorized());
        List<MvcResult> streams = new ArrayList<>();
        try {
            streams.add(mvc.perform(get("/api/chat/events").header("Authorization", login("client", "Client123!")))
                    .andExpect(request().asyncStarted()).andExpect(status().isOk()).andReturn());
            streams.add(mvc.perform(get("/api/chat/events").header("Authorization", login("chauffeur", "Chauffeur123!")))
                    .andExpect(request().asyncStarted()).andReturn());
            streams.add(mvc.perform(get("/api/chat/events").header("Authorization", login("entreprise", "Entreprise123!")))
                    .andExpect(request().asyncStarted()).andReturn());
            events.changed(new ChatService.Changed(user("client").getId(), user("chauffeur").getId(), "private-key"));
            for (int i = 0; i < 2; i++) assertThat(streams.get(i).getResponse().getContentAsString()).contains("event:changed").doesNotContain("private-key");
            assertThat(streams.get(2).getResponse().getContentAsString()).contains("event:ready").doesNotContain("event:changed");
        } finally { streams.forEach(result -> result.getRequest().getAsyncContext().complete()); }
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void liveStreamsDoNotRetainDatabaseConnections() throws Exception {
        String client = login("client", "Client123!");
        String driver = login("chauffeur", "Chauffeur123!");
        List<MvcResult> streams = new ArrayList<>();
        try {
            for (int i = 0; i < 12; i++) {
                streams.add(mvc.perform(get("/api/chat/events").header("Authorization", i % 2 == 0 ? client : driver))
                        .andExpect(request().asyncStarted()).andExpect(status().isOk()).andReturn());
                assertThat(dataSource.unwrap(com.zaxxer.hikari.HikariDataSource.class).getHikariPoolMXBean().getActiveConnections()).isZero();
            }
            assertThat(getJson("/api/chat/conversations", client).size()).isEqualTo(1);
        } finally { streams.forEach(result -> result.getRequest().getAsyncContext().complete()); }
    }
}
