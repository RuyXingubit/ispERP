package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.MarcoCivilApi;
import br.dev.xb.isperp.api.dto.MarcoCivilReportRequest;
import br.dev.xb.isperp.api.dto.MarcoCivilReportResponse;
import br.dev.xb.isperp.api.dto.MarcoCivilSearchRequest;
import br.dev.xb.isperp.api.dto.MarcoCivilSearchResult;
import br.dev.xb.isperp.service.MarcoCivilInvestigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class MarcoCivilController implements MarcoCivilApi {

    private final MarcoCivilInvestigationService marcoCivilInvestigationService;

    @Override
    @PostMapping({"/marco-civil/search", "/api/marco-civil/search"})
    public ResponseEntity<MarcoCivilSearchResult> searchMarcoCivilSubscriber(@RequestBody MarcoCivilSearchRequest request) {
        br.dev.xb.isperp.dto.MarcoCivilSearchRequest internalReq = br.dev.xb.isperp.dto.MarcoCivilSearchRequest.builder()
                .ip(request.getIp())
                .port(request.getPort())
                .timestamp(request.getTimestamp())
                .build();
        return ResponseEntity.ok(toApiResponse(marcoCivilInvestigationService.searchSubscriber(internalReq)));
    }

    @Override
    @PostMapping({"/marco-civil/reports", "/api/marco-civil/reports"})
    public ResponseEntity<MarcoCivilReportResponse> generateMarcoCivilReport(@RequestBody MarcoCivilReportRequest request) {
        br.dev.xb.isperp.dto.MarcoCivilReportRequest internalReq = br.dev.xb.isperp.dto.MarcoCivilReportRequest.builder()
                .courtOrderNumber(request.getCourtOrderNumber())
                .requesterAuthority(request.getRequesterAuthority())
                .queriedIp(request.getQueriedIp())
                .queriedPort(request.getQueriedPort())
                .queriedTimestamp(request.getQueriedTimestamp())
                .matchedContractId(request.getMatchedContractId())
                .notes(request.getNotes())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(marcoCivilInvestigationService.generateOfficialReport(internalReq)));
    }

    private MarcoCivilSearchResult toApiResponse(br.dev.xb.isperp.dto.MarcoCivilSearchResult internal) {
        if (internal == null) return null;
        MarcoCivilSearchResult res = new MarcoCivilSearchResult();
        res.setMatched(internal.isMatched());
        res.setQueriedIp(internal.getQueriedIp());
        res.setQueriedPort(internal.getQueriedPort());
        res.setQueriedTimestamp(internal.getQueriedTimestamp());
        res.setUsedCgnat(internal.isUsedCgnat());
        res.setResolvedPrivateIp(internal.getResolvedPrivateIp());
        res.setCgnatRuleSummary(internal.getCgnatRuleSummary());
        res.setRadacctId(internal.getRadacctId());
        res.setUsername(internal.getUsername());
        res.setCallingStationId(internal.getCallingStationId());
        res.setNasIpAddress(internal.getNasIpAddress());
        res.setSessionStartTime(internal.getSessionStartTime());
        res.setSessionStopTime(internal.getSessionStopTime());
        res.setContractId(internal.getContractId());
        res.setContractNumber(internal.getContractNumber());
        res.setCustomerId(internal.getCustomerId());
        res.setCustomerName(internal.getCustomerName());
        res.setCustomerCpfCnpj(internal.getCustomerCpfCnpj());
        res.setCustomerPhone(internal.getCustomerPhone());
        res.setCustomerEmail(internal.getCustomerEmail());
        res.setInstallationAddress(internal.getInstallationAddress());
        res.setPlanName(internal.getPlanName());
        return res;
    }

    private MarcoCivilReportResponse toApiResponse(br.dev.xb.isperp.dto.MarcoCivilReportResponse internal) {
        if (internal == null) return null;
        MarcoCivilReportResponse res = new MarcoCivilReportResponse();
        res.setId(internal.getId());
        res.setValidationToken(internal.getValidationToken());
        res.setSha256Hash(internal.getSha256Hash());
        res.setPublicValidationUrl(internal.getPublicValidationUrl());
        res.setQrCodePayload(internal.getQrCodePayload());
        res.setCourtOrderNumber(internal.getCourtOrderNumber());
        res.setRequesterAuthority(internal.getRequesterAuthority());
        res.setQueriedIp(internal.getQueriedIp());
        res.setQueriedPort(internal.getQueriedPort());
        res.setQueriedTimestamp(internal.getQueriedTimestamp());
        res.setMatchedContractId(internal.getMatchedContractId());
        res.setMatchedCustomerName(internal.getMatchedCustomerName());
        res.setMatchedCpfCnpj(internal.getMatchedCpfCnpj());
        res.setMatchedCallingStationId(internal.getMatchedCallingStationId());
        res.setMatchedSessionStart(internal.getMatchedSessionStart());
        res.setMatchedSessionStop(internal.getMatchedSessionStop());
        res.setReportPdfUrl(internal.getReportPdfUrl());
        res.setNotes(internal.getNotes());
        res.setCreatedAt(internal.getCreatedAt());
        return res;
    }
}
