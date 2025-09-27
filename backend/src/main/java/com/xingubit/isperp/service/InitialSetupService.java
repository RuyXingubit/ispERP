package com.xingubit.isperp.service;

import com.xingubit.isperp.dto.InitialSetupRequest;
import com.xingubit.isperp.entity.Company;
import com.xingubit.isperp.entity.SiteSettings;
import com.xingubit.isperp.entity.User;
import com.xingubit.isperp.repository.CompanyRepository;
import com.xingubit.isperp.repository.SiteSettingsRepository;
import com.xingubit.isperp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InitialSetupService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    public boolean isSetupCompleted() {
        // Verificar se todas as etapas do setup foram concluídas:
        // 1. Pelo menos um usuário administrador existe
        // 2. Pelo menos uma empresa foi cadastrada
        // 3. Configurações do site foram definidas
        return userRepository.count() > 0 && 
               companyRepository.count() > 0 && 
               siteSettingsRepository.count() > 0;
    }

    @Transactional
    public void performSetup(InitialSetupRequest request) {
        if (isSetupCompleted()) {
            throw new RuntimeException("Setup já foi realizado anteriormente");
        }

        // Criar usuário administrador
        createAdminUser(request);
        
        // Criar empresa
        createCompany(request);
        
        // Criar configurações do site
        createSiteSettings(request);
    }

    private void createAdminUser(InitialSetupRequest request) {
        User admin = new User();
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setPassword(hashPassword(request.getAdminPassword()));
        admin.setRole(User.UserRole.ADMIN);
        admin.setActive(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(admin);
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Método para hash da senha
    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private void createCompany(InitialSetupRequest request) {
        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setDocument(request.getCompanyCnpj());
        company.setAddress(request.getCompanyAddress());
        company.setPhone(request.getCompanyPhone());
        company.setEmail(request.getCompanyEmail());
        company.setWebsite(request.getCompanyWebsite());
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        
        companyRepository.save(company);
    }

    private void createSiteSettings(InitialSetupRequest request) {
        SiteSettings settings = new SiteSettings();
        settings.setSiteTitle(request.getSiteTitle());
        settings.setSiteDescription(request.getSiteDescription() != null ? request.getSiteDescription() : "");
        settings.setPrimaryColor(request.getPrimaryColor() != null ? request.getPrimaryColor() : "#1976d2");
        settings.setSecondaryColor(request.getSecondaryColor() != null ? request.getSecondaryColor() : "#dc004e");
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        
        siteSettingsRepository.save(settings);
    }
}