package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do plano é obrigatório")
    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @NotBlank(message = "Nome do cliente é obrigatório")
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @NotBlank(message = "CPF do cliente é obrigatório")
    @Column(name = "customer_cpf", nullable = false, length = 14)
    private String customerCpf;

    @Nullable
    @Column(name = "customer_email")
    private String customerEmail;

    @NotBlank(message = "Telefone do cliente é obrigatório")
    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @NotBlank(message = "Endereço de instalação é obrigatório")
    @Column(name = "installation_address", nullable = false)
    private String installationAddress;

    @NotBlank(message = "Cidade é obrigatória")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank(message = "Estado é obrigatório")
    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @NotBlank(message = "CEP é obrigatório")
    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @NotNull(message = "Dia de vencimento preferencial é obrigatório")
    @Column(name = "preferred_due_date", nullable = false)
    @Builder.Default
    private Integer preferredDueDate = 10;

    @Column(name = "notification_channel", nullable = false, length = 30)
    @Builder.Default
    private String notificationChannel = "WHATSAPP";

    @Nullable
    @Column(name = "seller_name", length = 150)
    private String sellerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SaleStatus status = SaleStatus.SUBMITTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Nullable
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
            this.status = SaleStatus.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum SaleStatus {
        SUBMITTED,
        PROCESSED,
        CANCELED
    }
}
