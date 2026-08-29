package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do cliente é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @NotNull(message = "ID do plano é obrigatório")
    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "sale_id")
    private UUID saleId;

    @NotBlank(message = "Número do contrato é obrigatório")
    @Column(name = "contract_number", nullable = false, unique = true, length = 50)
    private String contractNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ContractStatus status = ContractStatus.PENDING_INSTALLATION;

    @NotNull(message = "Mensalidade é obrigatória")
    @Positive(message = "Mensalidade deve ser positiva")
    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @NotNull(message = "Dia de vencimento é obrigatório")
    @Column(name = "due_day", nullable = false)
    @Builder.Default
    private Integer dueDay = 10;

    @Column(name = "custom_suspension_days")
    private Integer customSuspensionDays;

    @NotBlank(message = "Endereço de instalação é obrigatório")
    @Column(name = "installation_address", nullable = false)
    private String installationAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

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
        if (this.status == null) {
            this.status = ContractStatus.PENDING_INSTALLATION;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum ContractStatus {
        PENDING_INSTALLATION,
        ACTIVE,
        SUSPENDED,
        CANCELED
    }
}
