package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.AuditLogsApi;
import br.dev.xb.isperp.api.dto.AuditLogPageResponse;
import br.dev.xb.isperp.api.dto.AuditLogResponse;
import br.dev.xb.isperp.dto.AuditLogResponseDto;
import br.dev.xb.isperp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuditLogController implements AuditLogsApi {

    private final AuditLogService auditLogService;

    @Override
    public ResponseEntity<AuditLogPageResponse> getAuditLogs(
            UUID userId,
            String entityName,
            String action,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Integer page,
            Integer size) {

        int pageNum = page != null ? Math.max(page, 0) : 0;
        int pageSize = size != null ? Math.min(Math.max(size, 1), 100) : 20;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        LocalDateTime start = startDate != null ? startDate.toLocalDateTime() : null;
        LocalDateTime end = endDate != null ? endDate.toLocalDateTime() : null;

        Page<AuditLogResponseDto> results = auditLogService.searchLogs(
                userId,
                entityName,
                action,
                start,
                end,
                pageable
        );

        List<AuditLogResponse> content = results.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        AuditLogPageResponse response = new AuditLogPageResponse();
        response.setContent(content);
        response.setTotalElements(results.getTotalElements());
        response.setTotalPages(results.getTotalPages());
        response.setSize(results.getSize());
        response.setNumber(results.getNumber());

        return ResponseEntity.ok(response);
    }

    private AuditLogResponse toResponse(AuditLogResponseDto dto) {
        AuditLogResponse item = new AuditLogResponse();
        item.setId(dto.getId());
        item.setUserId(dto.getUserId());
        item.setUserName(dto.getUserName());
        item.setUserEmail(dto.getUserEmail());
        item.setAction(dto.getAction());
        item.setEntityName(dto.getEntityName());
        item.setEntityId(dto.getEntityId());
        item.setDetails(dto.getDetails());
        if (dto.getCreatedAt() != null) {
            item.setCreatedAt(dto.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        return item;
    }
}
