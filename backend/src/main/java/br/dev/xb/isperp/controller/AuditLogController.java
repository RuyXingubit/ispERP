package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.AuditLogResponseDto;
import br.dev.xb.isperp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int cappedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), cappedSize);

        Page<AuditLogResponseDto> results = auditLogService.searchLogs(
                userId,
                entityName,
                action,
                startDate,
                endDate,
                pageable
        );

        return ResponseEntity.ok(results);
    }
}
