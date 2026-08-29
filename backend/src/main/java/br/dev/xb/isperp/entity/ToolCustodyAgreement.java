package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tool_custody_agreements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCustodyAgreement {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Código do termo é obrigatório")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "work_order_id")
    private UUID workOrderId;

    @Column(name = "holder_user_id")
    private UUID holderUserId;

    @NotBlank(message = "Nome do responsável é obrigatório")
    @Column(name = "holder_name", nullable = false, length = 150)
    private String holderName;

    @NotBlank(message = "CPF do responsável é obrigatório")
    @Column(name = "holder_cpf", nullable = false, length = 20)
    private String holderCpf;

    @Column(name = "is_third_party", nullable = false)
    @Builder.Default
    private Boolean isThirdParty = false;

    @Column(name = "total_promissory_value", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalPromissoryValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.ACTIVE;

    @NotBlank(message = "Texto do termo é obrigatório")
    @Column(name = "agreement_text", columnDefinition = "text", nullable = false)
    private String agreementText;

    @Column(name = "dispatch_photo_url", columnDefinition = "text")
    private String dispatchPhotoUrl;

    @Column(name = "return_photo_url", columnDefinition = "text")
    private String returnPhotoUrl;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

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
        if (this.signedAt == null) {
            this.signedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AgreementStatus {
        ACTIVE,
        RETURNED_OK,
        RETURNED_DAMAGED,
        EXECUTED_JUDICIALLY
    }
}
