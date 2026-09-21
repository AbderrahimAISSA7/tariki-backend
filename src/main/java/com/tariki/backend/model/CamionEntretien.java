package com.tariki.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class CamionEntretien {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Camion camion;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Type type;
    private LocalDate echeanceDate;
    private Long echeanceKm;
    @Column(length = 2000)
    private String notes;
    private LocalDate effectueLe;
    private Long effectueKm;
    private Integer scorePneus;
    @Version
    private Long version;

    public enum Type { VIDANGE, PNEUS, FREINS, REVISION, AUTRE }
}
