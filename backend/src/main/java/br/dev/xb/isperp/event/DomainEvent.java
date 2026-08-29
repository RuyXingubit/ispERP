package br.dev.xb.isperp.event;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrato base para todos os eventos de domínio da aplicação.
 */
public interface DomainEvent {

    /**
     * Identificador único do evento (gerado como UUIDv7).
     */
    UUID getEventId();

    /**
     * Momento exato em que o evento ocorreu.
     */
    LocalDateTime getOccurredAt();

    /**
     * Tipo do evento (ex: SALE_SUBMITTED, CONTRACT_CREATED, WORK_ORDER_COMPLETED).
     */
    String getEventType();

    /**
     * Tipo do agregado que originou o evento (ex: Sale, Contract, WorkOrder, Invoice).
     */
    String getAggregateType();

    /**
     * Identificador do agregado raiz (em formato String/UUID).
     */
    String getAggregateId();

    /**
     * Dados ou DTO transportado pelo evento.
     */
    Object getPayload();
}

