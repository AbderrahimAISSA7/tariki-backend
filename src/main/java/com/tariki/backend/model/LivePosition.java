package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "live_position", indexes = @Index(name = "live_position_expiry_idx", columnList = "updated_at"))
@Getter @Setter @NoArgsConstructor
public class LivePosition {
    @Id
    private Long chauffeurId;
    @Version
    private Long version;
    @Column(nullable = false)
    private Long livraisonId;
    @Column(nullable = false)
    private Long entrepriseId;
    @Column(nullable = false)
    private Long clientId;
    @Column(nullable = false)
    private UUID sessionId;
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean sharingActive;
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Instant observedAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
