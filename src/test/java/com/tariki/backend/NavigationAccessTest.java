package com.tariki.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariki.backend.dto.NavigationDTO;
import com.tariki.backend.model.Livraison;
import com.tariki.backend.repository.LivraisonRepository;
import com.tariki.backend.service.NavigationRoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:navigation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never", "spring.jpa.show-sql=false", "logging.level.org.springframework.web=WARN"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class NavigationAccessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LivraisonRepository deliveries;
    @MockBean NavigationRoutingService routing;

    private String login(String role) throws Exception {
        String password = switch (role) { case "entreprise" -> "Entreprise123!"; case "chauffeur" -> "Chauffeur123!"; default -> "Client123!"; };
        return "Bearer " + json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("email", role + ".demo@tariki.ma", "password", password))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private Livraison delivery(String reference) {
        return deliveries.findAll().stream().filter(d -> reference.equals(d.getReference())).findFirst().orElseThrow();
    }
    private String path(Livraison d) { return "/api/livraisons/" + d.getId(); }
    private String destination(Livraison d) throws Exception {
        return json.writeValueAsString(Map.of("adresseLivraison", "Depot Figuig", "destinationLatitude", 32.108,
            "destinationLongitude", -1.229, "version", d.getVersion()));
    }
    private String origin() throws Exception {
        return json.writeValueAsString(new NavigationDTO.Origin(34.68, -1.91, 10.0, Instant.now()));
    }
    private Livraison located() {
        Livraison d = delivery("OT-2026-001");
        d.setAdresseLivraison("Depot Figuig"); d.setDestinationLatitude(32.108); d.setDestinationLongitude(-1.229);
        return deliveries.saveAndFlush(d);
    }

    @Test void companySetsDestinationAndRelatedProfilesCanReadIt() throws Exception {
        Livraison d = delivery("OT-2026-001");
        mvc.perform(patch(path(d) + "/destination").header("Authorization", login("entreprise")).contentType("application/json").content(destination(d)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.adresseLivraison").value("Depot Figuig"))
            .andExpect(jsonPath("$.destinationLatitude").value(32.108));
        for (String role : new String[]{"chauffeur", "client"}) {
            mvc.perform(get(path(d)).header("Authorization", login(role))).andExpect(status().isOk())
                .andExpect(jsonPath("$.destinationLongitude").value(-1.229));
            mvc.perform(patch(path(d) + "/destination").header("Authorization", login(role)).contentType("application/json").content(destination(d)))
                .andExpect(status().isForbidden());
        }
    }

    @Test void invalidPointsStaleVersionsAndCompletedDeliveriesAreRejected() throws Exception {
        Livraison d = delivery("OT-2026-001"); String company = login("entreprise");
        for (String payload : new String[]{destination(d).replace("32.108", "91"), destination(d).replace("-1.229", "181"),
            destination(d).replace("32.108", "null"), destination(d).replace("Depot Figuig", " ")}) {
            mvc.perform(patch(path(d) + "/destination").header("Authorization", company).contentType("application/json").content(payload)).andExpect(status().isBadRequest());
        }
        String old = destination(d);
        mvc.perform(patch(path(d) + "/destination").header("Authorization", company).contentType("application/json").content(old)).andExpect(status().isOk());
        mvc.perform(patch(path(d) + "/destination").header("Authorization", company).contentType("application/json").content(old)).andExpect(status().isConflict());
        Livraison ended = delivery("OT-2026-002");
        mvc.perform(patch(path(ended) + "/destination").header("Authorization", company).contentType("application/json").content(destination(ended))).andExpect(status().isConflict());
    }

    @Test void routingRequiresAssignedDriverActiveDeliveryAndDestination() throws Exception {
        Livraison d = delivery("OT-2026-001"); String route = path(d) + "/navigation/route";
        mvc.perform(post(route).contentType("application/json").content(origin())).andExpect(status().isUnauthorized());
        for (String role : new String[]{"entreprise", "client"}) {
            mvc.perform(post(route).header("Authorization", login(role)).contentType("application/json").content(origin())).andExpect(status().isForbidden());
        }
        String driver = login("chauffeur");
        mvc.perform(post(route).header("Authorization", driver).contentType("application/json").content(origin())).andExpect(status().isConflict());
        for (String reference : new String[]{"OT-2026-004", "OT-2026-002"}) {
            mvc.perform(post(path(delivery(reference)) + "/navigation/route").header("Authorization", driver).contentType("application/json").content(origin())).andExpect(status().isConflict());
        }
        mvc.perform(post(path(delivery("OT-2026-006")) + "/navigation/route").header("Authorization", driver).contentType("application/json").content(origin())).andExpect(status().isNotFound());
        verifyNoInteractions(routing);
    }

    @Test void routingUsesStoredDestinationWithoutChangingDeliveryStatus() throws Exception {
        Livraison d = located();
        when(routing.route(any(), any())).thenReturn(json.readTree("{\"distance\":100,\"duration\":20}"));
        mvc.perform(post(path(d) + "/navigation/route").header("Authorization", login("chauffeur")).contentType("application/json").content(origin()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.distance").value(100));
        var target = org.mockito.ArgumentCaptor.forClass(NavigationDTO.Target.class);
        verify(routing).route(any(), target.capture());
        assertThat(target.getValue().latitude()).isEqualTo(d.getDestinationLatitude());
        assertThat(target.getValue().longitude()).isEqualTo(d.getDestinationLongitude());
        assertThat(d.getStatut()).isEqualTo("EN_COURS");
    }

    @Test void routingRejectsIncompleteOriginBeforeContactingProvider() throws Exception {
        Livraison d = located();
        mvc.perform(post(path(d) + "/navigation/route").header("Authorization", login("chauffeur")).contentType("application/json").content("{\"latitude\":32,\"longitude\":-2}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(routing);
    }
}
