package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.SiteSettingsApi;
import br.dev.xb.isperp.api.dto.SiteSettingsResponse;
import br.dev.xb.isperp.api.dto.SiteSettingsUpdateRequest;
import br.dev.xb.isperp.entity.SiteSettings;
import br.dev.xb.isperp.service.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SiteSettingsController implements SiteSettingsApi {

    private final SiteSettingsService siteSettingsService;

    @Override
    public ResponseEntity<SiteSettingsResponse> getSiteSettings() {
        return siteSettingsService.getSiteSettings()
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<SiteSettingsResponse> updateSiteSettings(SiteSettingsUpdateRequest request) {
        SiteSettings entity = SiteSettings.builder()
                .siteTitle(request.getSiteTitle())
                .siteDescription(request.getSiteDescription())
                .primaryColor(request.getPrimaryColor())
                .secondaryColor(request.getSecondaryColor())
                .build();

        SiteSettings saved = siteSettingsService.updateSiteSettings(entity);
        return ResponseEntity.ok(toResponse(saved));
    }

    private SiteSettingsResponse toResponse(SiteSettings entity) {
        SiteSettingsResponse response = new SiteSettingsResponse();
        response.setId(entity.getId());
        response.setSiteTitle(entity.getSiteTitle());
        response.setSiteDescription(entity.getSiteDescription());
        response.setPrimaryColor(entity.getPrimaryColor());
        response.setSecondaryColor(entity.getSecondaryColor());
        if (entity.getCreatedAt() != null) {
            response.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            response.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return response;
    }
}
