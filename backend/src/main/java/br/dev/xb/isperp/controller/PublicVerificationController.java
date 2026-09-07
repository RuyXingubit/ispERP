package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.PublicVerificationApi;
import br.dev.xb.isperp.api.dto.PublicValidationResponse;
import br.dev.xb.isperp.service.MarcoCivilInvestigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class PublicVerificationController implements PublicVerificationApi {

    private final MarcoCivilInvestigationService marcoCivilInvestigationService;

    @Override
    @GetMapping({"/public/marco-civil/validate/{token}", "/api/public/marco-civil/validate/{token}"})
    public ResponseEntity<PublicValidationResponse> validateMarcoCivilReport(@PathVariable String token) {
        return ResponseEntity.ok(toApiResponse(marcoCivilInvestigationService.validatePublicToken(token)));
    }

    private PublicValidationResponse toApiResponse(br.dev.xb.isperp.dto.PublicValidationResponse internal) {
        if (internal == null) return null;
        PublicValidationResponse res = new PublicValidationResponse();
        res.setValid(internal.isValid());
        res.setValidationToken(internal.getValidationToken());
        res.setSha256Hash(internal.getSha256Hash());
        res.setCourtOrderNumber(internal.getCourtOrderNumber());
        res.setRequesterAuthority(internal.getRequesterAuthority());
        res.setQueriedIp(internal.getQueriedIp());
        res.setQueriedPort(internal.getQueriedPort());
        res.setQueriedTimestamp(internal.getQueriedTimestamp());
        res.setCustomerNameMasked(internal.getCustomerNameMasked());
        res.setCustomerCpfCnpjMasked(internal.getCustomerCpfCnpjMasked());
        res.setCallingStationId(internal.getCallingStationId());
        res.setReportIssuedAt(internal.getReportIssuedAt());
        res.setStatusMessage(internal.getStatusMessage());
        return res;
    }
}
