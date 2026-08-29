package br.dev.xb.isperp.notification.whatsapp;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class WhatsAppProviderResolver {

    private final List<WhatsAppProvider> providers;
    private final NotificationConfigRepository configRepository;

    /**
     * Resolve o provedor de WhatsApp ativo e sua respectiva configuração.
     *
     * @param preferredType Tipo de provedor preferido (opcional)
     * @return Par contendo o Provedor e a Configuração
     */
    public ResolvedWhatsAppProvider resolve(WhatsAppProviderType preferredType) {
        WhatsAppProviderType targetType = (preferredType != null) ? preferredType : WhatsAppProviderType.TWILIO;

        WhatsAppProvider provider = providers.stream()
                .filter(p -> p.getProviderType() == targetType)
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(p -> p.getProviderType() == WhatsAppProviderType.TWILIO)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Nenhum provedor de WhatsApp disponível")));

        Optional<NotificationConfig> configOpt = configRepository.findFirstByProviderTypeAndActiveTrue(provider.getProviderType());
        NotificationConfig config = configOpt.orElseGet(() ->
                configRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                        .orElseGet(() -> NotificationConfig.builder()
                                .name("Twilio Padrão")
                                .providerType(WhatsAppProviderType.TWILIO)
                                .accountSid("AC_default_sid")
                                .authToken("auth_token_default")
                                .fromPhoneNumber("whatsapp:+14155238886")
                                .active(true)
                                .build()
                        )
        );

        return new ResolvedWhatsAppProvider(provider, config);
    }

    public record ResolvedWhatsAppProvider(WhatsAppProvider provider, NotificationConfig config) {}
}
