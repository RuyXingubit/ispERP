package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fiscal_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class FiscalCompany {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @NotBlank(message = "CNPJ é obrigatório")
    @Size(max = 18)
    @Column(name = "cnpj", nullable = false, unique = true, length = 18)
    private String cnpj;

    @NotBlank(message = "Razão Social é obrigatória")
    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    @Nullable
    private String nomeFantasia;

    @NotBlank(message = "Inscrição Estadual é obrigatória")
    @Column(name = "inscricao_estadual", nullable = false, length = 30)
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = 30)
    @Nullable
    private String inscricaoMunicipal;

    @Column(name = "cnae_principal", nullable = false, length = 20)
    @Builder.Default
    private String cnaePrincipal = "6110-8/03";

    @Column(name = "regime_tributario", nullable = false, length = 30)
    @Builder.Default
    private String regimeTributario = "SIMPLES_NACIONAL";

    @Column(name = "aliquota_icms", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaIcms = BigDecimal.ZERO;

    @Column(name = "aliquota_fust", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaFust = new BigDecimal("0.65");

    @Column(name = "aliquota_funttel", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaFunttel = new BigDecimal("0.50");

    @Column(name = "aliquota_pis", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaPis = new BigDecimal("0.65");

    @Column(name = "aliquota_cofins", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaCofins = new BigDecimal("3.00");

    @NotBlank(message = "Logradouro é obrigatório")
    @Column(name = "logradouro", nullable = false)
    private String logradouro;

    @NotBlank(message = "Número é obrigatório")
    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @Column(name = "complemento", length = 100)
    @Nullable
    private String complemento;

    @NotBlank(message = "Bairro é obrigatório")
    @Column(name = "bairro", nullable = false, length = 100)
    private String bairro;

    @NotBlank(message = "Cidade é obrigatória")
    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade;

    @NotBlank(message = "UF é obrigatória")
    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @NotBlank(message = "CEP é obrigatório")
    @Column(name = "cep", nullable = false, length = 10)
    private String cep;

    @NotBlank(message = "Código IBGE é obrigatório")
    @Column(name = "codigo_ibge", nullable = false, length = 10)
    private String codigoIbge;

    @Column(name = "telefone", length = 30)
    @Nullable
    private String telefone;

    @Column(name = "email_fiscal")
    @Nullable
    private String emailFiscal;

    @Column(name = "nfcom_ambiente", nullable = false, length = 20)
    @Builder.Default
    private String nfcomAmbiente = "HOMOLOGACAO";

    @Column(name = "nfcom_serie", nullable = false, length = 10)
    @Builder.Default
    private String nfcomSerie = "1";

    @Column(name = "nfcom_proximo_numero", nullable = false)
    @Builder.Default
    private Integer nfcomProximoNumero = 1;

    @Column(name = "has_certificate", nullable = false)
    @Builder.Default
    private Boolean hasCertificate = false;

    @Column(name = "certificate_expires_at")
    @Nullable
    private LocalDateTime certificateExpiresAt;

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
