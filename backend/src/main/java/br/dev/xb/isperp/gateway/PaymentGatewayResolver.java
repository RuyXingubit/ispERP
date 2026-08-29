package br.dev.xb.isperp.gateway;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.repository.PaymentGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class PaymentGatewayResolver {

    private final List<PaymentGateway> gateways;
    private final PaymentGatewayConfigRepository configRepository;

    /**
     * Resolve o PaymentGateway adequado e sua respectiva configuração ativa.
     *
     * @param preferredType Tipo de gateway preferido (opcional)
     * @return Par contendo o Gateway e sua Configuração
     */
    public ResolvedGateway resolve(PaymentGatewayType preferredType) {
        PaymentGatewayType targetType = (preferredType != null) ? preferredType : PaymentGatewayType.XINGUBIT_PAY;

        PaymentGateway gateway = gateways.stream()
                .filter(g -> g.getGatewayType() == targetType)
                .findFirst()
                .orElseGet(() -> gateways.stream()
                        .filter(g -> g.getGatewayType() == PaymentGatewayType.XINGUBIT_PAY)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Nenhum gateway de pagamento disponível")));

        Optional<PaymentGatewayConfig> configOpt = configRepository.findFirstByGatewayTypeAndActiveTrue(gateway.getGatewayType());
        PaymentGatewayConfig config = configOpt.orElseGet(() ->
                configRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                        .orElseGet(() -> PaymentGatewayConfig.builder()
                                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                                .name("Xingubit Pay Padrão")
                                .apiKey("xb_api_default")
                                .secretKey("xb_secret_default")
                                .pixKey("pix@xingubit.com.br")
                                .sandbox(true)
                                .active(true)
                                .build()
                        )
        );

        return new ResolvedGateway(gateway, config);
    }

    public record ResolvedGateway(PaymentGateway gateway, PaymentGatewayConfig config) {}
}
