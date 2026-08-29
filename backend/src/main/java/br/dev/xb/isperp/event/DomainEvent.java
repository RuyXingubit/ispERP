package br.dev.xb.isperp.event;

import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrato base para todos os eventos de domínio da aplicação.
 */
public interface DomainEvent {

    /**
     * Identificador único do evento (gerado como UUIDv7).
     */
    @NonNull
    UUID getEventId();

    /**
     * Momento exato em que o evento ocorreu.
     */
    @NonNull
    LocalDateTime getOccurredAt();

    /**
     * Tipo do evento (ex: SALE_SUBMITTED, CONTRACT_CREATED, WORK_ORDER_COMPLETED).
     */
    @NonNull
    String getEventType();

    /**
     * Tipo do agregado que originou o evento (ex: Sale, Contract, WorkOrder, Invoice).
     */
    @NonNull
    String getAggregateType();

    /**
     * Identificador do agregado raiz (em formato String/UUID).
     */
    @NonNull
    String getAggregateId();

    /**
     * Dados ou DTO transportado pelo evento.
     */
    @NonNull
    Object getPayload();
}
