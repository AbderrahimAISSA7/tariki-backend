package com.tariki.backend.service;

import com.tariki.backend.dto.ChatDTO.*;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import com.tariki.backend.security.AccessScope;
import com.tariki.backend.security.ChatEncryption;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ChatService {
    private final AccessScope scope;
    private final UserRepository users;
    private final LivraisonRepository deliveries;
    private final ChatMessageRepository messages;
    private final ChatEncryption encryption;
    private final ApplicationEventPublisher events;

    public ChatService(AccessScope scope, UserRepository users, LivraisonRepository deliveries, ChatMessageRepository messages,
                       ChatEncryption encryption, ApplicationEventPublisher events) {
        this.scope = scope;
        this.users = users;
        this.deliveries = deliveries;
        this.messages = messages;
        this.encryption = encryption;
        this.events = events;
    }

    private record Channel(String key, String kind, User peer, String context, Livraison delivery) { }
    public record Changed(Long firstUser, Long secondUser, String channel) { }

    public List<Conversation> conversations() {
        User viewer = scope.currentUser();
        return channels(viewer).stream().map(c -> new Conversation(c.key(), c.kind(), c.peer().getId(), name(c.peer()), c.context(),
                c.delivery() == null ? null : c.delivery().getId(), c.delivery() == null ? null : c.delivery().getReference(),
                c.delivery() == null ? null : c.delivery().getStatut(),
                messages.findFirstByChannelKeyOrderByIdDesc(c.key()).map(ChatMessage::getSentAt).orElse(null),
                messages.countByChannelKeyAndRecipientIdAndReadAtIsNull(c.key(), viewer.getId())))
                .sorted(Comparator.comparing(Conversation::lastMessageAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Conversation::peerName).thenComparing(Conversation::key)).toList();
    }

    public History history(String key, Long before) {
        channel(scope.currentUser(), key);
        if (before != null && before <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curseur invalide");
        List<ChatMessage> page = messages.findByChannelKeyAndIdLessThanOrderByIdDesc(key, before == null ? Long.MAX_VALUE : before, PageRequest.of(0, 51));
        boolean hasMore = page.size() > 50;
        List<Message> result = new ArrayList<>(page.stream().limit(50).map(this::dto).toList());
        Collections.reverse(result);
        return new History(result, hasMore, result.isEmpty() ? null : result.get(0).id());
    }

    @Transactional
    public Message send(String key, Send request) {
        User current = scope.currentUser();
        // Serialize sends per account so retried requests cannot insert duplicates.
        User viewer = users.lockForChat(current.getId()).orElseThrow(this::notFound);
        Channel channel = channel(viewer, key);
        String text = request.content().strip();
        Optional<ChatMessage> previous = messages.findByChannelKeyAndSenderIdAndClientMessageId(key, viewer.getId(), request.clientMessageId());
        if (previous.isPresent()) {
            Message result = dto(previous.get());
            if (!result.content().equals(text)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Identifiant de message deja utilise");
            return result;
        }
        ChatMessage message = new ChatMessage();
        message.setChannelKey(key);
        message.setSenderId(viewer.getId());
        message.setRecipientId(channel.peer().getId());
        message.setClientMessageId(request.clientMessageId());
        message.setEncryptedBody(encryption.encrypt(text, key, viewer.getId(), request.clientMessageId()));
        message.setSentAt(Instant.now());
        messages.save(message);
        events.publishEvent(new Changed(viewer.getId(), channel.peer().getId(), key));
        return dto(message);
    }

    @Transactional
    public void read(String key, Long throughId) {
        User viewer = scope.currentUser();
        Channel channel = channel(viewer, key);
        if (messages.markRead(key, viewer.getId(), throughId, Instant.now()) > 0) {
            events.publishEvent(new Changed(viewer.getId(), channel.peer().getId(), key));
        }
    }

    private Channel channel(User viewer, String key) {
        return channels(viewer).stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow(this::notFound);
    }

    private List<Channel> channels(User viewer) {
        List<Channel> result = new ArrayList<>();
        if (viewer.getEntreprise() != null && (viewer.getRole() == User.Role.ENTREPRISE || viewer.getRole() == User.Role.CHAUFFEUR)) {
            User.Role otherRole = viewer.getRole() == User.Role.ENTREPRISE ? User.Role.CHAUFFEUR : User.Role.ENTREPRISE;
            users.findByEntrepriseIdAndRole(viewer.getEntreprise().getId(), otherRole).forEach(peer -> {
                Long manager = viewer.getRole() == User.Role.ENTREPRISE ? viewer.getId() : peer.getId();
                Long driver = viewer.getRole() == User.Role.CHAUFFEUR ? viewer.getId() : peer.getId();
                result.add(new Channel("internal-" + viewer.getEntreprise().getId() + "-" + manager + "-" + driver,
                        "INTERNAL", peer, viewer.getEntreprise().getNom(), null));
            });
        }
        List<Livraison> assigned = viewer.getRole() == User.Role.CHAUFFEUR ? deliveries.findByChauffeurId(viewer.getId())
                : viewer.getRole() == User.Role.CLIENT ? deliveries.findByClientId(viewer.getId()) : List.of();
        for (Livraison delivery : assigned) {
            if (delivery.getChauffeur() == null || delivery.getClient() == null
                    || delivery.getChauffeur().getRole() != User.Role.CHAUFFEUR || delivery.getClient().getRole() != User.Role.CLIENT) continue;
            User peer = viewer.getRole() == User.Role.CLIENT ? delivery.getChauffeur() : delivery.getClient();
            // Including both participants prevents a replacement driver from inheriting private history.
            String key = "delivery-" + delivery.getId() + "-" + delivery.getChauffeur().getId() + "-" + delivery.getClient().getId();
            result.add(new Channel(key, "DELIVERY", peer, delivery.getVilleDepart() + " - " + delivery.getVilleArrivee(), delivery));
        }
        return result;
    }

    private Message dto(ChatMessage m) {
        return new Message(m.getId(), m.getClientMessageId(), m.getSenderId(),
                encryption.decrypt(m.getEncryptedBody(), m.getChannelKey(), m.getSenderId(), m.getClientMessageId()), m.getSentAt(), m.getReadAt());
    }

    private String name(User user) {
        if (user instanceof Chauffeur driver) return driver.getPrenom() + " " + driver.getNom();
        if (user instanceof Client client) return client.getPrenom() + " " + client.getNom();
        if (user instanceof ResponsableEntreprise manager) return manager.getPrenom() + " " + manager.getNom();
        return user.getUsername();
    }

    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable"); }
}
