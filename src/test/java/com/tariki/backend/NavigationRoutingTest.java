package com.tariki.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tariki.backend.dto.NavigationDTO;
import com.tariki.backend.service.NavigationRoutingService;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class NavigationRoutingTest {
    private HttpServer server;
    private NavigationRoutingService service;
    private String body = "{\"code\":\"Ok\",\"routes\":[{\"distance\":120,\"duration\":30,\"geometry\":{\"coordinates\":[[-2,32],[-1,33]]},\"legs\":[{\"steps\":[]}]}]}";
    private int status = 200;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> uri = new AtomicReference<>(), agent = new AtomicReference<>(), auth = new AtomicReference<>();
    private final NavigationDTO.Target target = new NavigationDTO.Target(1L, 33, -1);
    private NavigationDTO.Origin origin() { return new NavigationDTO.Origin(32.0, -2.0, 10.0, Instant.now()); }

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet(); uri.set(exchange.getRequestURI().toString());
            agent.set(exchange.getRequestHeaders().getFirst("User-Agent")); auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        service = new NavigationRoutingService(new ObjectMapper(), "http://127.0.0.1:" + server.getAddress().getPort(), "TarikiNavigationTest");
    }
    @AfterEach void stop() { server.stop(0); }
    @Test void coordinatesAreLongitudeFirstWithStepsAndNoAuthenticationSentUpstream() {
        assertThat(service.route(origin(), target).path("distance").asInt()).isEqualTo(120);
        assertThat(uri.get()).contains("/-2.000000,32.000000;-1.000000,33.000000?").contains("steps=true").contains("geometries=geojson");
        assertThat(agent.get()).isEqualTo("TarikiNavigationTest"); assertThat(auth.get()).isNull();
    }
    @Test void invalidOrStaleFixesNeverLeaveTheServer() {
        for (var origin : new NavigationDTO.Origin[]{new NavigationDTO.Origin(91.0, -2.0, 10.0, Instant.now()),
            new NavigationDTO.Origin(Double.NaN, -2.0, 10.0, Instant.now()), new NavigationDTO.Origin(32.0, -2.0, 101.0, Instant.now()),
            new NavigationDTO.Origin(32.0, -2.0, 10.0, Instant.now().minusSeconds(31)), new NavigationDTO.Origin(32.0, -2.0, 10.0, Instant.now().plusSeconds(60))}) {
            assertThatThrownBy(() -> service.route(origin, target)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        }
        assertThat(calls.get()).isZero();
    }
    @Test void routingRequestsAreRateLimitedAcrossDrivers() {
        service.route(origin(), target);
        for (var destination : new NavigationDTO.Target[]{target, new NavigationDTO.Target(2L, 33, -1)}) {
            assertThatThrownBy(() -> service.route(origin(), destination)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(429));
        }
        assertThat(calls.get()).isEqualTo(1);
    }
    @Test void unreachableDestinationHasAnExplicitError() {
        body = "{\"code\":\"NoRoute\"}";
        assertThatThrownBy(() -> service.route(origin(), target)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(422));
    }
    @Test void upstreamFailureIsNotPresentedAsARoute() {
        status = 503;
        assertThatThrownBy(() -> service.route(origin(), target)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }
    @Test void malformedResponseIsNotPresentedAsARoute() {
        body = "{}";
        assertThatThrownBy(() -> service.route(origin(), target)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }
}
