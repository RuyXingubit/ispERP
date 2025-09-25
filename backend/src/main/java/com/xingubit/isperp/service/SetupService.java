package com.xingubit.isperp.service;

import com.xingubit.isperp.dto.SetupRequest;
import com.xingubit.isperp.dto.SetupResponse;
import com.xingubit.isperp.entity.*;
import com.xingubit.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupService {
    
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final SetupStatusRepository setupStatusRepository;
    private final PasswordEncoder passwordEncoder;
    
    public SetupResponse getSetupStatus() {
        SetupStatus setupStatus = setupStatusRepository.findFirstByOrderByIdAsc()
                .orElse(SetupStatus.builder()
                        .isSetupCompleted(false)
                        .setupStep(0)
                        .build());
        
        if (setupStatus.getIsSetupCompleted()) {
            return SetupResponse.success("Setup já foi concluído");
        }
        
        return SetupResponse.inProgress(setupStatus.getSetupStep(), 
                "Setup ainda não foi concluído");
    }
    
    @Transactional
    public SetupResponse performSetup(SetupRequest request) {
        log.info("Iniciando processo de setup inicial");
        
        // Verificar se o setup já foi concluído
        SetupStatus setupStatus = setupStatusRepository.findFirstByOrderByIdAsc()
                .orElse(SetupStatus.builder()
                        .isSetupCompleted(false)
                        .setupStep(0)
                        .build());
        
        if (setupStatus.getIsSetupCompleted()) {
            return SetupResponse.success("Setup já foi concluído anteriormente");
        }
        
        try {
            // 1. Criar usuário administrador
            createAdminUser(request);
            log.info("Usuário administrador criado com sucesso");
            
            // 2. Criar informações da empresa
            createCompany(request);
            log.info("Informações da empresa criadas com sucesso");
            
            // 3. Criar configurações do site
            createSiteSettings(request);
            log.info("Configurações do site criadas com sucesso");
            
            // 4. Marcar setup como concluído
            setupStatus.setIsSetupCompleted(true);
            setupStatus.setSetupStep(3);
            setupStatusRepository.save(setupStatus);
            
            log.info("Setup inicial concluído com sucesso");
            return SetupResponse.success("Setup inicial concluído com sucesso!");
            
        } catch (Exception e) {
            log.error("Erro durante o processo de setup: {}", e.getMessage(), e);
            throw new RuntimeException("Erro durante o setup: " + e.getMessage());
        }
    }
    
    private void createAdminUser(SetupRequest request) {
        // Verificar se já existe um usuário com o email
        if (userRepository.existsByEmail(request.getAdminEmail())) {
            throw new RuntimeException("Já existe um usuário com este email");
        }
        
        User adminUser = User.builder()
                .name(request.getAdminName())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .role(User.UserRole.ADMIN)
                .active(true)
                .build();
        
        userRepository.save(adminUser);
    }
    
    private void createCompany(SetupRequest request) {
        Company company = Company.builder()
                .name(request.getCompanyName())
                .cnpj(request.getCompanyCnpj())
                .address(request.getCompanyAddress())
                .phone(request.getCompanyPhone())
                .email(request.getCompanyEmail())
                .website(request.getCompanyWebsite())
                .build();
        
        companyRepository.save(company);
    }
    
    private void createSiteSettings(SetupRequest request) {
        SiteSettings siteSettings = SiteSettings.builder()
                .siteTitle(request.getSiteTitle())
                .siteDescription(request.getSiteDescription())
                .primaryColor(request.getPrimaryColor() != null ? request.getPrimaryColor() : "#1976d2")
                .secondaryColor(request.getSecondaryColor() != null ? request.getSecondaryColor() : "#dc004e")
                .build();
        
        siteSettingsRepository.save(siteSettings);
    }
}