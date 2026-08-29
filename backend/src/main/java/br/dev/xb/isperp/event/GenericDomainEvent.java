package br.dev.xb.isperp.event;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("null")
public class GenericDomainEvent implements DomainEvent {

    @Builder.Default
    private UUID eventId = UuidCreatorUtils.generateUuidV7();

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private Object payload;

    @Override
    public UUID getEventId() {
        return eventId != null ? eventId : UuidCreatorUtils.generateUuidV7();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt != null ? occurredAt : LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return eventType != null ? eventType : "UNKNOWN";
    }

    @Override
    public String getAggregateType() {
        return aggregateType != null ? aggregateType : "UNKNOWN";
    }

    @Override
    public String getAggregateId() {
        return aggregateId != null ? aggregateId : "";
    }

    @Override
    public Object getPayload() {
        return payload != null ? payload : "";
    }
}
