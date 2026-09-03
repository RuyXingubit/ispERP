package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "network_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkProject {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "neighborhood", nullable = false, length = 150)
    private String neighborhood;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "budget_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "target_subscribers", nullable = false)
    @Builder.Default
    private Integer targetSubscribers = 100;

    @Column(name = "start_date", nullable = false)
    @Builder.Default
    private LocalDate startDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.status == null) {
            this.status = ProjectStatus.ACTIVE;
        }
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
