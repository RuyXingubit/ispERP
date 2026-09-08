package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.dto.InitialSetupRequest;
import br.dev.xb.isperp.entity.Company;
import br.dev.xb.isperp.entity.SiteSettings;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.repository.CompanyRepository;
import br.dev.xb.isperp.repository.SiteSettingsRepository;
import br.dev.xb.isperp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@SuppressWarnings("null")
public class InitialSetupService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    @Autowired
    private br.dev.xb.isperp.repository.PaymentGatewayConfigRepository paymentGatewayConfigRepository;

    @Autowired
    private br.dev.xb.isperp.repository.PlanRepository planRepository;

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

        // 1. Criar usuário administrador
        createAdminUser(request);
        
        // 2. Criar empresa
        Company company = createCompany(request);
        
        // 3. Criar configurações do site
        createSiteSettings(request);

        // 4. Inicializar configuração padrão do Xingubit Pay
        createDefaultPaymentGateway(company);

        // 5. Inicializar plano de internet padrão caso não exista
        createDefaultPlan();
    }

    private void createAdminUser(InitialSetupRequest request) {
        User admin = new User();
        admin.setId(br.dev.xb.isperp.util.UuidCreatorUtils.generateUuidV7());
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setPassword(hashPassword(request.getAdminPassword()));
        admin.setRole(UserRole.ADMIN);
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

    private Company createCompany(InitialSetupRequest request) {
        Company company = new Company();
        company.setId(br.dev.xb.isperp.util.UuidCreatorUtils.generateUuidV7());
        company.setName(request.getCompanyName());
        company.setDocument(request.getCompanyCnpj());
        company.setAddress(request.getCompanyAddress());
        company.setPhone(request.getCompanyPhone());
        company.setEmail(request.getCompanyEmail());
        company.setWebsite(request.getCompanyWebsite());
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        
        return companyRepository.save(company);
    }

    private void createSiteSettings(InitialSetupRequest request) {
        SiteSettings settings = new SiteSettings();
        settings.setId(br.dev.xb.isperp.util.UuidCreatorUtils.generateUuidV7());
        settings.setSiteTitle(request.getSiteTitle());
        settings.setSiteDescription(request.getSiteDescription() != null ? request.getSiteDescription() : "");
        settings.setPrimaryColor(request.getPrimaryColor() != null ? request.getPrimaryColor() : "#1976d2");
        settings.setSecondaryColor(request.getSecondaryColor() != null ? request.getSecondaryColor() : "#dc004e");
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        
        siteSettingsRepository.save(settings);
    }

    private void createDefaultPaymentGateway(Company company) {
        if (paymentGatewayConfigRepository.count() == 0) {
            var config = br.dev.xb.isperp.entity.PaymentGatewayConfig.builder()
                    .id(br.dev.xb.isperp.util.UuidCreatorUtils.generateUuidV7())
                    .companyId(company.getId())
                    .gatewayType(br.dev.xb.isperp.gateway.PaymentGatewayType.XINGUBIT_PAY)
                    .name("Xingubit Pay Oficial")
                    .apiKey("")
                    .secretKey("")
                    .webhookSecret("")
                    .pixKey("")
                    .sandbox(false)
                    .active(true)
                    .build();
            paymentGatewayConfigRepository.save(config);
        }
    }

    private void createDefaultPlan() {
        if (planRepository.count() == 0) {
            var defaultPlan = br.dev.xb.isperp.entity.Plan.builder()
                    .id(br.dev.xb.isperp.util.UuidCreatorUtils.generateUuidV7())
                    .name("Fibra 500 Mega")
                    .downloadSpeed(500)
                    .uploadSpeed(250)
                    .price(new java.math.BigDecimal("99.90"))
                    .description("Plano residencial com fibra óptica de alta velocidade e Wi-Fi 6")
                    .suspensionDays(15)
                    .alwaysIssueNfcom(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            planRepository.save(defaultPlan);
        }
    }
}