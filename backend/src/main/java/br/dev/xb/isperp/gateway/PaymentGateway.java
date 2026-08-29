package br.dev.xb.isperp.gateway;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.dto.CreateChargeRequest;
import br.dev.xb.isperp.gateway.dto.CreateChargeResponse;

import java.util.Map;

public interface PaymentGateway {

    /**
     * Identificador do tipo de gateway suportado por esta implementação.
     */
    PaymentGatewayType getGatewayType();

    /**
     * Emite uma cobrança no gateway (Pix COB/COBV, Boleto ou Cartão).
     *
     * @param request Dados da cobrança
     * @param config Configuração e credenciais do gateway
     * @return Dados de pagamento (Pix Copia e Cola, QR Code, txId)
     */
    CreateChargeResponse createCharge(CreateChargeRequest request, PaymentGatewayConfig config);

    /**
     * Processa e valida um webhook assíncrono recebido do gateway.
     *
     * @param payload Mapa com o corpo da requisição JSON do webhook
     * @param signature Assinatura do header para validação criptográfica (HMAC-SHA256)
     * @param config Configuração do gateway contendo o webhookSecret
     * @return txId ou identificador da fatura confirmada
     */
    String processWebhook(Map<String, Object> payload, String signature, PaymentGatewayConfig config);

    /**
     * Cancela uma cobrança no gateway.
     *
     * @param externalTransactionId ID da transação no gateway
     * @param config Configuração do gateway
     * @return true se cancelado com sucesso
     */
    boolean cancelCharge(String externalTransactionId, PaymentGatewayConfig config);
}
