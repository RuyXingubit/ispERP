package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRoute {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Código da rota é obrigatório")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "technician_user_id")
    private UUID technicianUserId;

    @NotNull(message = "Data da rota é obrigatória")
    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Column(name = "total_distance_km", precision = 8, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RouteStatus status = RouteStatus.PLANNED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RouteStatus {
        PLANNED,
        DISPATCHED,
        IN_PROGRESS,
        COMPLETED,
        CANCELED
    }
}
