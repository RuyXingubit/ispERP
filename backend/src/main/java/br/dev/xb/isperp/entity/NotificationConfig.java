package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
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
@Table(name = "notification_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class NotificationConfig {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Tipo de provedor é obrigatório")
    @Column(name = "provider_type", nullable = false, length = 50)
    @Builder.Default
    private WhatsAppProviderType providerType = WhatsAppProviderType.TWILIO;

    @NotBlank(message = "Nome da configuração é obrigatório")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "api_token")
    private String apiToken;

    @Column(name = "account_sid")
    private String accountSid;

    @Column(name = "auth_token")
    private String authToken;

    @Column(name = "from_phone_number", length = 50)
    private String fromPhoneNumber;

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
