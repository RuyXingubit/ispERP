package br.dev.xb.isperp.gateway;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.xingubit.XingubitPayGateway;
import br.dev.xb.isperp.repository.PaymentGatewayConfigRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayResolverTest {

    @Mock
    private PaymentGatewayConfigRepository configRepository;

    private PaymentGatewayResolver resolver;
    private XingubitPayGateway xingubitPayGateway;
    private PaymentGatewayConfig sampleConfig;

    @BeforeEach
    void setUp() {
        xingubitPayGateway = new XingubitPayGateway();
        resolver = new PaymentGatewayResolver(List.of(xingubitPayGateway), configRepository);

        sampleConfig = PaymentGatewayConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                .name("Xingubit Pay Oficial")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve resolver XingubitPayGateway quando solicitado")
    void shouldResolveXingubitPayGateway() {
        when(configRepository.findFirstByGatewayTypeAndActiveTrue(PaymentGatewayType.XINGUBIT_PAY))
                .thenReturn(Optional.of(sampleConfig));

        PaymentGatewayResolver.ResolvedGateway resolved = resolver.resolve(PaymentGatewayType.XINGUBIT_PAY);

        assertNotNull(resolved);
        assertEquals(PaymentGatewayType.XINGUBIT_PAY, resolved.gateway().getGatewayType());
        assertEquals("Xingubit Pay Oficial", resolved.config().getName());
    }
}
