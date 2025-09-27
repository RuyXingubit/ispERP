package com.xingubit.isperp.controller;

import com.xingubit.isperp.dto.InitialSetupRequest;
import com.xingubit.isperp.service.InitialSetupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/initial-setup")
@CrossOrigin(origins = "*")
public class InitialSetupController {

    @Autowired
    private InitialSetupService initialSetupService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSetupStatus() {
        boolean isCompleted = initialSetupService.isSetupCompleted();
        return ResponseEntity.ok(Map.of("isSetupCompleted", isCompleted));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> performInitialSetup(@Valid @RequestBody InitialSetupRequest request) {
        try {
            if (initialSetupService.isSetupCompleted()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Setup já foi realizado anteriormente"));
            }

            initialSetupService.performSetup(request);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "isSetupCompleted", true,
                "message", "Setup realizado com sucesso!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Erro durante o setup: " + e.getMessage()));
        }
    }
}