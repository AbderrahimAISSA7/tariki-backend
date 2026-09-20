package com.tariki.backend.controller;

import com.tariki.backend.dto.ChatDTO.*;
import com.tariki.backend.security.AccessScope;
import com.tariki.backend.security.JwtService;
import com.tariki.backend.service.ChatEvents;
import com.tariki.backend.service.ChatService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chat;
    private final ChatEvents events;
    private final AccessScope scope;
    private final JwtService jwt;

    public ChatController(ChatService chat, ChatEvents events, AccessScope scope, JwtService jwt) {
        this.chat = chat;
        this.events = events;
        this.scope = scope;
        this.jwt = jwt;
    }

    @GetMapping("/conversations")
    public List<Conversation> conversations() { return chat.conversations(); }

    @GetMapping("/conversations/{key}/messages")
    public History history(@PathVariable String key, @RequestParam(required = false) Long before) { return chat.history(key, before); }

    @PostMapping("/conversations/{key}/messages")
    public Message send(@PathVariable String key, @Valid @RequestBody Send request) { return chat.send(key, request); }

    @PostMapping("/conversations/{key}/read")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void read(@PathVariable String key, @Valid @RequestBody Read request) { chat.read(key, request.throughId()); }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@RequestHeader("Authorization") String authorization, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Accel-Buffering", "no");
        return events.subscribe(scope.currentUser().getId(), jwt.extractExpiration(authorization.substring(7)).getTime());
    }
}
