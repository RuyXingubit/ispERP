package com.isperp.service;

import com.xingubit.isperp.entity.SiteSettings;
import com.xingubit.isperp.repository.SiteSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SiteSettingsService {

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    public Optional<SiteSettings> getSiteSettings() {
        return siteSettingsRepository.findFirstByOrderByIdAsc();
    }

    public SiteSettings updateSiteSettings(SiteSettings settingsDetails) {
        Optional<SiteSettings> existingSettings = siteSettingsRepository.findFirstByOrderByIdAsc();
        
        SiteSettings settings;
        if (existingSettings.isPresent()) {
            settings = existingSettings.get();
        } else {
            settings = new SiteSettings();
        }

        settings.setSiteTitle(settingsDetails.getSiteTitle());
        settings.setSiteDescription(settingsDetails.getSiteDescription());
        settings.setPrimaryColor(settingsDetails.getPrimaryColor());
        settings.setSecondaryColor(settingsDetails.getSecondaryColor());

        return siteSettingsRepository.save(settings);
    }
}