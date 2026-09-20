package com.tariki.backend.service;

import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class ChatEvents {
    private record Connection(Long userId, SseEmitter emitter, long expiresAt) { }
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();

    public synchronized SseEmitter subscribe(Long userId, long tokenExpiresAt) {
        long lifetime = Math.min(55000, tokenExpiresAt - System.currentTimeMillis());
        if (lifetime <= 0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (connections.stream().filter(c -> c.userId().equals(userId)).count() >= 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de connexions de messagerie");
        }
        SseEmitter emitter = new SseEmitter(lifetime);
        Connection connection = new Connection(userId, emitter, System.currentTimeMillis() + lifetime);
        connections.add(connection);
        emitter.onCompletion(() -> connections.remove(connection));
        emitter.onTimeout(() -> close(connection));
        emitter.onError(error -> connections.remove(connection));
        send(connection, "ready");
        return emitter;
    }

    @TransactionalEventListener
    public void changed(ChatService.Changed event) {
        connections.stream().filter(c -> c.userId().equals(event.firstUser()) || c.userId().equals(event.secondUser()))
                .forEach(c -> send(c, "changed"));
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() { connections.forEach(c -> send(c, "heartbeat")); }

    private void send(Connection c, String event) {
        if (System.currentTimeMillis() >= c.expiresAt()) { close(c); return; }
        try {
            // Notifications contain no content or conversation metadata; REST rechecks membership.
            c.emitter().send(SseEmitter.event().name(event).data(Map.of()));
        } catch (IOException | IllegalStateException ex) { close(c); }
    }

    private void close(Connection c) {
        connections.remove(c);
        c.emitter().complete();
    }

    @PreDestroy
    public void shutdown() { connections.forEach(this::close); }
}
