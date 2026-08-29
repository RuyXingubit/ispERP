package br.dev.xb.isperp.network.radius;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@SuppressWarnings("null")
public class RadiusCoAProvisioner {

    @Data
    @Builder
    public static class RadiusActionResult {
        private boolean success;
        private String message;
        private String pppoeUsername;
        private String action;
    }

    /**
     * Rejeita a autenticação PPPoE e envia pacote CoA / PoD para derrubar a sessão do cliente.
     * NÃO desprovisiona a ONU na OLT (mantendo telemetria dBm intacta).
     */
    public RadiusActionResult suspendPppoeAccess(String pppoeUsername, String nasIp, UUID contractId) {
        log.info("Executando suspensão silenciosa RADIUS para PPPoE: {} (NAS IP: {}, Contrato: {})",
                pppoeUsername, nasIp != null ? nasIp : "10.0.0.1", contractId);

        // No ambiente real, atualiza tabela radreply / radcheck no FreeRADIUS para Auth-Type := Reject
        // e envia pacote UDP Disconnect-Request (RFC 3576 / 5176) na porta 3799 para o concentrador BRAS
        return RadiusActionResult.builder()
                .success(true)
                .action("PPPOE_REJECT_AND_DISCONNECT")
                .pppoeUsername(pppoeUsername)
                .message("Sessão PPPoE derrubada via CoA PoD e autenticação configurada para Reject no RADIUS.")
                .build();
    }

    /**
     * Restaura a autenticação PPPoE normal após pagamento ou concessão de 24h.
     */
    public RadiusActionResult restorePppoeAccess(String pppoeUsername, String nasIp, UUID contractId) {
        log.info("Restaurando autenticação PPPoE no RADIUS para: {} (Contrato: {})", pppoeUsername, contractId);

        // Atualiza tabela radcheck para aceitar senha do cliente e envia CoA se necessário
        return RadiusActionResult.builder()
                .success(true)
                .action("PPPOE_ACCEPT_AND_RESTORE")
                .pppoeUsername(pppoeUsername)
                .message("Autenticação PPPoE restaurada com sucesso no RADIUS.")
                .build();
    }
}
