package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.SiteSettings;
import br.dev.xb.isperp.repository.SiteSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SiteSettingsService {

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    public Optional<SiteSettings> getSiteSettings() {
        return siteSettingsRepository.findFirstByOrderByCreatedAtAsc();
    }

    public Optional<SiteSettings> getSiteSettingsById(@NonNull UUID id) {
        return siteSettingsRepository.findById(id);
    }

    public SiteSettings updateSiteSettings(@NonNull SiteSettings siteSettings) {
        Optional<SiteSettings> existing = siteSettingsRepository.findFirstByOrderByCreatedAtAsc();
        if (existing.isPresent()) {
            SiteSettings current = existing.get();
            current.setSiteTitle(siteSettings.getSiteTitle());
            current.setSiteDescription(siteSettings.getSiteDescription());
            current.setPrimaryColor(siteSettings.getPrimaryColor());
            current.setSecondaryColor(siteSettings.getSecondaryColor());
            return siteSettingsRepository.save(current);
        }
        return siteSettingsRepository.save(siteSettings);
    }
}
