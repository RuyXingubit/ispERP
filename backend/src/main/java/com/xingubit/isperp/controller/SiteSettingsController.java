package com.xingubit.isperp.controller;

import com.xingubit.isperp.entity.SiteSettings;
import com.xingubit.isperp.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/site-settings")
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
    public ResponseEntity<SiteSettings> updateSiteSettings(@Valid @RequestBody SiteSettings siteSettings) {
        try {
            SiteSettings updated = siteSettingsService.updateSiteSettings(siteSettings);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
