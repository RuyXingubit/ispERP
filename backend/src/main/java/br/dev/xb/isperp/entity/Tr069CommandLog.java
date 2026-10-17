package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tr069_command_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tr069CommandLog {

    @Id
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "onu_serial", length = 64)
    private String onuSerial;

    @Column(name = "command_type", nullable = false, length = 32)
    private String commandType;

    @Column(name = "origin_channel", nullable = false, length = 32)
    private String originChannel;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "summary_message", length = 500)
    private String summaryMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
