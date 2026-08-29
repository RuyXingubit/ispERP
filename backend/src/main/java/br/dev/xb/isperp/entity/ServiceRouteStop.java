package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_route_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRouteStop {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID da rota é obrigatório")
    @Column(name = "route_id", nullable = false)
    private UUID routeId;

    @NotNull(message = "ID da ordem de serviço é obrigatório")
    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @NotNull(message = "Ordem na sequência é obrigatória")
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
    }
}
