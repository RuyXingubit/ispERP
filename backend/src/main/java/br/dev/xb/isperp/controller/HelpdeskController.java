package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.HelpdeskTicket;
import br.dev.xb.isperp.entity.TicketInteraction;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.service.HelpdeskService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/helpdesk/tickets", "/api/helpdesk/tickets"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    @PostMapping
    public ResponseEntity<HelpdeskTicket> createTicket(@RequestBody HelpdeskService.CreateTicketRequest request) {
        HelpdeskTicket ticket = helpdeskService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping
    public ResponseEntity<List<HelpdeskTicket>> getAllTickets() {
        return ResponseEntity.ok(helpdeskService.getAllTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HelpdeskTicket> getTicketById(@PathVariable UUID id) {
        return helpdeskService.getTicketById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/protocol/{protocol}")
    public ResponseEntity<HelpdeskTicket> getTicketByProtocol(@PathVariable String protocol) {
        return helpdeskService.getTicketByProtocol(protocol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<HelpdeskTicket>> getTicketsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(helpdeskService.getTicketsByCustomer(customerId));
    }

    @PostMapping("/{id}/escalate-n2")
    public ResponseEntity<HelpdeskTicket> escalateToN2(
            @PathVariable UUID id,
            @RequestBody EscalateN2Request request) {
        HelpdeskTicket ticket = helpdeskService.escalateToN2(
                id, request.getAttendantUserId(), request.getAttendantName(), request.getReason());
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/resolve-n2")
    public ResponseEntity<HelpdeskTicket> resolveByN2(
            @PathVariable UUID id,
            @RequestBody ResolveN2Request request) {
        HelpdeskTicket ticket = helpdeskService.resolveByN2(
                id, request.getN2UserId(), request.getN2Name(), request.getResolutionNotes());
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/escalate-work-order")
    public ResponseEntity<WorkOrder> escalateToWorkOrder(
            @PathVariable UUID id,
            @RequestBody EscalateWorkOrderRequest request) {
        WorkOrder workOrder = helpdeskService.escalateToWorkOrder(
                id, request.getN2UserId(), request.getN2Name(), request.getTechnicalReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrder);
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<TicketInteraction> addInteraction(
            @PathVariable UUID id,
            @RequestBody HelpdeskService.AddInteractionRequest request) {
        TicketInteraction interaction = helpdeskService.addInteraction(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(interaction);
    }

    @GetMapping("/{id}/interactions")
    public ResponseEntity<List<TicketInteraction>> getInteractions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean includeInternal) {
        return ResponseEntity.ok(helpdeskService.getInteractions(id, includeInternal));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<HelpdeskTicket> closeTicket(
            @PathVariable UUID id,
            @RequestBody CloseTicketRequest request) {
        HelpdeskTicket ticket = helpdeskService.closeTicket(
                id, request.getSatisfactionRating(), request.getClosureNotes());
        return ResponseEntity.ok(ticket);
    }

    @Data
    @Builder
    public static class EscalateN2Request {
        private UUID attendantUserId;
        private String attendantName;
        private String reason;
    }

    @Data
    @Builder
    public static class ResolveN2Request {
        private UUID n2UserId;
        private String n2Name;
        private String resolutionNotes;
    }

    @Data
    @Builder
    public static class EscalateWorkOrderRequest {
        private UUID n2UserId;
        private String n2Name;
        private String technicalReason;
    }

    @Data
    @Builder
    public static class CloseTicketRequest {
        private Integer satisfactionRating;
        private String closureNotes;
    }
}
