package com.isperp.controller;

import com.xingubit.isperp.entity.SiteSettings;
import com.isperp.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/site-settings")
@CrossOrigin(origins = "*")
public class SiteSettingsController {

    @Autowired
    private SiteSettingsService siteSettingsService;

    @GetMapping
    public ResponseEntity<SiteSettings> getSiteSettings() {
        Optional<SiteSettings> settings = siteSettingsService.getSiteSettings();
        return settings.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettings> updateSiteSettings(@Valid @RequestBody SiteSettings settingsDetails) {
        try {
            SiteSettings updatedSettings = siteSettingsService.updateSiteSettings(settingsDetails);
            return ResponseEntity.ok(updatedSettings);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}