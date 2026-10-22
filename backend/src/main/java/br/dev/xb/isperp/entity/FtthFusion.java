package br.dev.xb.isperp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "ftth_fusions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FtthFusion implements OffsetAuditableEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(name = "closure_id", nullable = false)
    private UUID closureId;

    @Builder.Default
    @Column(name = "tray_number", nullable = false)
    private int trayNumber = 1;

    // Fibra de Origem (Entrada)
    @Column(name = "source_cable_id", nullable = false)
    private UUID sourceCableId;

    @Column(name = "source_fiber_number", nullable = false)
    private int sourceFiberNumber;

    // Destino A: Outra Fibra de Saída
    @Column(name = "target_cable_id")
    private @Nullable UUID targetCableId;

    @Column(name = "target_fiber_number")
    private @Nullable Integer targetFiberNumber;

    // Destino B: Entrada de Splitter
    @Column(name = "target_splitter_id")
    private @Nullable UUID targetSplitterId;

    // Destino C: Alimentação Direta de CTO
    @Column(name = "target_cto_id")
    private @Nullable UUID targetCtoId;

    @Builder.Default
    @Column(name = "loss_db", nullable = false, precision = 4, scale = 2)
    private BigDecimal lossDb = new BigDecimal("0.05");

    @Column(columnDefinition = "TEXT")
    private @Nullable String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
