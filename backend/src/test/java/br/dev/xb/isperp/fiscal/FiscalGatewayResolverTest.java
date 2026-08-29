package br.dev.xb.isperp.fiscal;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.mock.MockFiscalDriver;
import br.dev.xb.isperp.fiscal.xingubit.XingubitPayFiscalDriver;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalGatewayConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FiscalGatewayResolverTest {

    private FiscalGatewayConfigRepository configRepository;
    private FiscalCompanyRepository companyRepository;
    private FiscalGatewayResolver resolver;

    @BeforeEach
    void setUp() {
        configRepository = Mockito.mock(FiscalGatewayConfigRepository.class);
        companyRepository = Mockito.mock(FiscalCompanyRepository.class);

        XingubitPayFiscalDriver xingubitDriver = new XingubitPayFiscalDriver();
        MockFiscalDriver mockDriver = new MockFiscalDriver();

        resolver = new FiscalGatewayResolver(
                List.of(xingubitDriver, mockDriver),
                configRepository,
                companyRepository
        );
    }

    @Test
    @DisplayName("Deve resolver o driver Xingubit Pay quando configurado")
    void testResolveXingubitDriver() {
        UUID companyId = UUID.randomUUID();
        FiscalCompany company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Empresa Teste")
                .build();

        FiscalGatewayConfig config = FiscalGatewayConfig.builder()
                .companyId(companyId)
                .gatewayType(FiscalGatewayType.XINGUBIT_PAY)
                .isActive(true)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(configRepository.findByCompanyIdAndIsActiveTrue(companyId)).thenReturn(Optional.of(config));

        FiscalGatewayResolver.ResolvedFiscalGateway resolved = resolver.resolve(companyId);

        assertNotNull(resolved);
        assertEquals(FiscalGatewayType.XINGUBIT_PAY, resolved.gateway().getGatewayType());
        assertEquals(companyId, resolved.company().getId());
    }

    @Test
    @DisplayName("Deve resolver o driver Mock quando configurado")
    void testResolveMockDriver() {
        UUID companyId = UUID.randomUUID();
        FiscalCompany company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Empresa Teste")
                .build();

        FiscalGatewayConfig config = FiscalGatewayConfig.builder()
                .companyId(companyId)
                .gatewayType(FiscalGatewayType.MOCK)
                .isActive(true)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(configRepository.findByCompanyIdAndIsActiveTrue(companyId)).thenReturn(Optional.of(config));

        FiscalGatewayResolver.ResolvedFiscalGateway resolved = resolver.resolve(companyId);

        assertNotNull(resolved);
        assertEquals(FiscalGatewayType.MOCK, resolved.gateway().getGatewayType());
    }
}
