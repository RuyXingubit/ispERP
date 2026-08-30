package br.dev.xb.isperp.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "marco_civil_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcoCivilReport {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "validation_token", nullable = false, unique = true, length = 64)
    private String validationToken;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "court_order_number", length = 100)
    private @Nullable String courtOrderNumber;

    @Column(name = "requester_authority", length = 150)
    private @Nullable String requesterAuthority;

    @Column(name = "queried_ip", nullable = false, length = 45)
    private String queriedIp;

    @Column(name = "queried_port")
    private @Nullable Integer queriedPort;

    @Column(name = "queried_timestamp", nullable = false)
    private OffsetDateTime queriedTimestamp;

    @Column(name = "matched_contract_id")
    private @Nullable UUID matchedContractId;

    @Column(name = "matched_customer_name", length = 150)
    private @Nullable String matchedCustomerName;

    @Column(name = "matched_cpf_cnpj", length = 20)
    private @Nullable String matchedCpfCnpj;

    @Column(name = "matched_calling_station_id", length = 50)
    private @Nullable String matchedCallingStationId;

    @Column(name = "matched_session_start")
    private @Nullable OffsetDateTime matchedSessionStart;

    @Column(name = "matched_session_stop")
    private @Nullable OffsetDateTime matchedSessionStop;

    @Column(name = "report_pdf_url", columnDefinition = "TEXT")
    private @Nullable String reportPdfUrl;

    @Column(columnDefinition = "TEXT")
    private @Nullable String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Objects.requireNonNull(UuidCreator.getTimeOrderedEpoch());
        }
    }
}
