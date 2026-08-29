package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nfcom_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class NfcomRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "invoice_id")
    @Nullable
    private UUID invoiceId;

    @Column(name = "contract_id")
    @Nullable
    private UUID contractId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "chave_acesso", length = 44, unique = true)
    @Nullable
    private String chaveAcesso;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "serie", nullable = false, length = 10)
    @Builder.Default
    private String serie = "1";

    @Column(name = "modelo", nullable = false, length = 10)
    @Builder.Default
    private String modelo = "62";

    @Column(name = "tipo_emissao", nullable = false, length = 20)
    @Builder.Default
    private String tipoEmissao = "NORMAL";

    @Column(name = "ambiente", nullable = false, length = 20)
    @Builder.Default
    private String ambiente = "HOMOLOGACAO";

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "EMITIDA";

    @Column(name = "protocolo_autorizacao", length = 60)
    @Nullable
    private String protocoloAutorizacao;

    @Column(name = "data_autorizacao")
    @Nullable
    private LocalDateTime dataAutorizacao;

    @Column(name = "digest_value", length = 100)
    @Nullable
    private String digestValue;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "valor_icms", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorIcms = BigDecimal.ZERO;

    @Column(name = "valor_fust", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorFust = BigDecimal.ZERO;

    @Column(name = "valor_funttel", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorFunttel = BigDecimal.ZERO;

    @Column(name = "xml_autorizado", columnDefinition = "TEXT")
    @Nullable
    private String xmlAutorizado;

    @Column(name = "danfe_pdf_url", length = 500)
    @Nullable
    private String danfePdfUrl;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    @Nullable
    private String motivoCancelamento;

    @Column(name = "data_cancelamento")
    @Nullable
    private LocalDateTime dataCancelamento;

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
