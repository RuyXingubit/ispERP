package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.HelpdeskApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.HelpdeskTicket;
import br.dev.xb.isperp.entity.TicketInteraction;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.HelpdeskMapper;
import br.dev.xb.isperp.mapper.WorkOrderMapper;
import br.dev.xb.isperp.service.HelpdeskService;
import jakarta.validation.Valid;
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
public class HelpdeskController implements HelpdeskApi {

    private final HelpdeskService helpdeskService;
    private final HelpdeskMapper helpdeskMapper;
    private final WorkOrderMapper workOrderMapper;

    @Override
    @PostMapping
    public ResponseEntity<HelpdeskTicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        try {
            HelpdeskService.CreateTicketRequest serviceReq = helpdeskMapper.toCreateServiceRequest(request);
            HelpdeskTicket ticket = helpdeskService.createTicket(serviceReq);
            return ResponseEntity.status(HttpStatus.CREATED).body(helpdeskMapper.toResponse(ticket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @GetMapping
    public ResponseEntity<List<HelpdeskTicketResponse>> getAllTickets() {
        return ResponseEntity.ok(helpdeskMapper.toResponseList(helpdeskService.getAllTickets()));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<HelpdeskTicketResponse> getTicketById(@PathVariable UUID id) {
        return helpdeskService.getTicketById(id)
                .map(helpdeskMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @GetMapping("/protocol/{protocol}")
    public ResponseEntity<HelpdeskTicketResponse> getTicketByProtocol(@PathVariable String protocol) {
        return helpdeskService.getTicketByProtocol(protocol)
                .map(helpdeskMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<HelpdeskTicketResponse>> getTicketsByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(helpdeskMapper.toResponseList(helpdeskService.getTicketsByCustomer(customerId)));
    }

    @Override
    @PostMapping("/{id}/escalate-n2")
    public ResponseEntity<HelpdeskTicketResponse> escalateToN2(
            @PathVariable UUID id,
            @Valid @RequestBody EscalateN2Request request) {
        try {
            HelpdeskTicket ticket = helpdeskService.escalateToN2(
                    id, request.getAttendantUserId(), request.getAttendantName(), request.getReason());
            return ResponseEntity.ok(helpdeskMapper.toResponse(ticket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @PostMapping("/{id}/resolve-n2")
    public ResponseEntity<HelpdeskTicketResponse> resolveByN2(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveN2Request request) {
        try {
            HelpdeskTicket ticket = helpdeskService.resolveByN2(
                    id, request.getN2UserId(), request.getN2Name(), request.getResolutionNotes());
            return ResponseEntity.ok(helpdeskMapper.toResponse(ticket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @PostMapping("/{id}/escalate-work-order")
    public ResponseEntity<WorkOrderResponse> escalateToWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody EscalateWorkOrderRequest request) {
        try {
            WorkOrder workOrder = helpdeskService.escalateToWorkOrder(
                    id, request.getN2UserId(), request.getN2Name(), request.getTechnicalReason());
            return ResponseEntity.status(HttpStatus.CREATED).body(workOrderMapper.toResponse(workOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @PostMapping("/{id}/interactions")
    public ResponseEntity<TicketInteractionResponse> addInteraction(
            @PathVariable UUID id,
            @Valid @RequestBody TicketInteractionCreateRequest request) {
        try {
            HelpdeskService.AddInteractionRequest serviceReq = helpdeskMapper.toAddInteractionServiceRequest(request);
            TicketInteraction interaction = helpdeskService.addInteraction(id, serviceReq);
            return ResponseEntity.status(HttpStatus.CREATED).body(helpdeskMapper.toInteractionResponse(interaction));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    @GetMapping("/{id}/interactions")
    public ResponseEntity<List<TicketInteractionResponse>> getInteractions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") Boolean includeInternal) {
        boolean include = includeInternal == null || includeInternal;
        return ResponseEntity.ok(helpdeskMapper.toInteractionResponseList(helpdeskService.getInteractions(id, include)));
    }

    @Override
    @PostMapping("/{id}/close")
    public ResponseEntity<HelpdeskTicketResponse> closeTicket(
            @PathVariable UUID id,
            @Valid @RequestBody CloseTicketRequest request) {
        try {
            HelpdeskTicket ticket = helpdeskService.closeTicket(
                    id, request.getSatisfactionRating(), request.getClosureNotes());
            return ResponseEntity.ok(helpdeskMapper.toResponse(ticket));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
