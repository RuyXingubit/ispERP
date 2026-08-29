package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fiscal_gateway_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class FiscalGatewayConfig {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_type", nullable = false, length = 50)
    @Builder.Default
    private FiscalGatewayType gatewayType = FiscalGatewayType.XINGUBIT_PAY;

    @Column(name = "environment", nullable = false, length = 20)
    @Builder.Default
    private String environment = "HOMOLOGACAO";

    @Column(name = "client_id")
    @Nullable
    private String clientId;

    @Column(name = "client_secret")
    @Nullable
    private String clientSecret;

    @Column(name = "api_key")
    @Nullable
    private String apiKey;

    @Column(name = "base_url")
    @Builder.Default
    private String baseUrl = "https://pay.xingubit.com.br";

    @Column(name = "webhook_secret")
    @Nullable
    private String webhookSecret;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Nullable
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Nullable
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
