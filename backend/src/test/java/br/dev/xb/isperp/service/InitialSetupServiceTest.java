package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.InitialSetupRequest;
import br.dev.xb.isperp.entity.Company;
import br.dev.xb.isperp.entity.SiteSettings;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.repository.CompanyRepository;
import br.dev.xb.isperp.repository.SiteSettingsRepository;
import br.dev.xb.isperp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class InitialSetupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private SiteSettingsRepository siteSettingsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InitialSetupService initialSetupService;

    @Test
    @DisplayName("Deve verificar que o setup não foi realizado quando tabelas estão vazias")
    void shouldReportSetupNotCompletedWhenEmpty() {
        when(userRepository.count()).thenReturn(0L);

        assertFalse(initialSetupService.isSetupCompleted());
    }

    @Test
    @DisplayName("Deve verificar que o setup foi realizado quando há dados nas 3 entidades")
    void shouldReportSetupCompletedWhenPopulated() {
        when(userRepository.count()).thenReturn(1L);
        when(companyRepository.count()).thenReturn(1L);
        when(siteSettingsRepository.count()).thenReturn(1L);

        assertTrue(initialSetupService.isSetupCompleted());
    }

    @Test
    @DisplayName("Deve executar o setup inicial criando admin, empresa e configurações com sucesso")
    void shouldPerformSetupSuccessfully() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$encodedPassword");

        InitialSetupRequest request = InitialSetupRequest.builder()
                .adminName("Super Admin")
                .adminEmail("admin@provedor.com")
                .adminPassword("senha123456")
                .companyName("Fibra Telecom")
                .companyCnpj("12.345.678/0001-90")
                .companyPhone("11999999999")
                .companyEmail("contato@fibratelecom.com")
                .companyAddress("Av. Paulista, 1000")
                .companyWebsite("https://fibratelecom.com")
                .siteTitle("Fibra Telecom - Internet Ultrarrápida")
                .siteDescription("Melhor internet da região")
                .primaryColor("#0066cc")
                .secondaryColor("#ff6600")
                .build();

        initialSetupService.performSetup(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(companyRepository, times(1)).save(any(Company.class));
        verify(siteSettingsRepository, times(1)).save(any(SiteSettings.class));
    }
}
