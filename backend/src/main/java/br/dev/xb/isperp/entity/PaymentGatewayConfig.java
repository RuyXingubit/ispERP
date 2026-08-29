package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_gateway_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayConfig {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Tipo de gateway é obrigatório")
    @Column(name = "gateway_type", nullable = false, length = 50)
    @Builder.Default
    private PaymentGatewayType gatewayType = PaymentGatewayType.XINGUBIT_PAY;

    @NotBlank(message = "Nome da configuração é obrigatório")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "pix_key", length = 100)
    private String pixKey;

    @Column(name = "sandbox", nullable = false)
    @Builder.Default
    private Boolean sandbox = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

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
}
