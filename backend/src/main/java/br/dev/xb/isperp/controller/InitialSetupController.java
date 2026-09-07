package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.InitialSetupApi;
import br.dev.xb.isperp.api.dto.InitialSetupRequest;
import br.dev.xb.isperp.api.dto.InitialSetupResponse;
import br.dev.xb.isperp.api.dto.InitialSetupStatusResponse;
import br.dev.xb.isperp.service.InitialSetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping({"/initial-setup", "/setup"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InitialSetupController implements InitialSetupApi {

    private final InitialSetupService initialSetupService;

    @Override
    @GetMapping({"/status", ""})
    public ResponseEntity<InitialSetupStatusResponse> getSetupStatus() {
        boolean isCompleted = initialSetupService.isSetupCompleted();
        InitialSetupStatusResponse response = new InitialSetupStatusResponse();
        response.setIsSetupCompleted(isCompleted);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping({"", "/complete"})
    public ResponseEntity<InitialSetupResponse> performInitialSetup(@Valid @RequestBody InitialSetupRequest request) {
        try {
            if (initialSetupService.isSetupCompleted()) {
                InitialSetupResponse response = new InitialSetupResponse();
                response.setSuccess(false);
                response.setIsSetupCompleted(true);
                response.setMessage("Setup já foi realizado anteriormente");
                return ResponseEntity.badRequest().body(response);
            }

            br.dev.xb.isperp.dto.InitialSetupRequest internalReq = br.dev.xb.isperp.dto.InitialSetupRequest.builder()
                    .adminName(request.getAdminName())
                    .adminEmail(request.getAdminEmail())
                    .adminPassword(request.getAdminPassword())
                    .companyName(request.getCompanyName())
                    .companyCnpj(request.getCompanyCnpj())
                    .companyAddress(request.getCompanyAddress())
                    .companyPhone(request.getCompanyPhone())
                    .companyEmail(request.getCompanyEmail())
                    .companyWebsite(request.getCompanyWebsite())
                    .siteTitle(request.getSiteTitle())
                    .siteDescription(request.getSiteDescription())
                    .primaryColor(request.getPrimaryColor())
                    .secondaryColor(request.getSecondaryColor())
                    .build();

            initialSetupService.performSetup(internalReq);

            InitialSetupResponse response = new InitialSetupResponse();
            response.setSuccess(true);
            response.setIsSetupCompleted(true);
            response.setMessage("Setup realizado com sucesso!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            InitialSetupResponse response = new InitialSetupResponse();
            response.setSuccess(false);
            response.setIsSetupCompleted(false);
            response.setMessage("Erro durante o setup: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}