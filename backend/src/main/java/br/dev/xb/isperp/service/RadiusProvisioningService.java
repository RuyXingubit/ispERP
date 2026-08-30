package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.RadCheck;
import br.dev.xb.isperp.entity.RadReply;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.repository.RadCheckRepository;
import br.dev.xb.isperp.repository.RadReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RadiusProvisioningService {

    private final RadCheckRepository radCheckRepository;
    private final RadReplyRepository radReplyRepository;

    @Transactional
    public void provisionUser(
            String username,
            String cleartextPassword,
            long downloadMbps,
            long uploadMbps,
            NasVendorType vendor,
            @Nullable String fixedIp,
            @Nullable String ipv6Prefix
    ) {
        provisionSubscriber(username, cleartextPassword, downloadMbps, uploadMbps, vendor, fixedIp, ipv6Prefix, false);
    }

    @Transactional
    public void blockUser(String username, NasVendorType vendor, @Nullable String reason) {
        log.info("Aplicando bloqueio de acesso RADIUS para {}. Motivo: {}", username, reason);
        // Mantém a senha existente ou usa padrão se não houver
        String password = radCheckRepository.findByUsername(username).stream()
                .filter(c -> "Cleartext-Password".equals(c.getAttribute()))
                .map(RadCheck::getValue)
                .findFirst()
                .orElse("xb123456");

        provisionSubscriber(username, password, 0, 0, vendor, null, null, true);
    }

    @Transactional
    public void unblockUser(
            String username,
            long downloadMbps,
            long uploadMbps,
            NasVendorType vendor,
            @Nullable String fixedIp,
            @Nullable String ipv6Prefix
    ) {
        log.info("Restaurando acesso total RADIUS para {} ({}M/{}M)", username, downloadMbps, uploadMbps);
        String password = radCheckRepository.findByUsername(username).stream()
                .filter(c -> "Cleartext-Password".equals(c.getAttribute()))
                .map(RadCheck::getValue)
                .findFirst()
                .orElse("xb123456");

        provisionSubscriber(username, password, downloadMbps, uploadMbps, vendor, fixedIp, ipv6Prefix, false);
    }

    @Transactional
    public void provisionSubscriber(
            String username,
            String cleartextPassword,
            long downloadMbps,
            long uploadMbps,
            NasVendorType vendor,
            @Nullable String fixedIp,
            @Nullable String ipv6Prefix,
            boolean blocked
    ) {
        log.info("Provisionando assinante no FreeRADIUS: {} (Vendor: {}, Bloqueado: {})", username, vendor, blocked);

        // 1. Limpa registros anteriores do usuário
        radCheckRepository.deleteByUsername(username);
        radReplyRepository.deleteByUsername(username);

        // 2. Cria Check de Autenticação (Cleartext-Password)
        RadCheck check = RadCheck.builder()
                .username(username)
                .attribute("Cleartext-Password")
                .op(":=")
                .value(cleartextPassword)
                .build();
        radCheckRepository.save(check);

        // 3. Monta Atributos de Resposta (Reply)
        List<RadReply> replies = new ArrayList<>();

        if (blocked) {
            // Perfil de Bloqueio por Inadimplência
            replies.addAll(buildBlockedAttributes(username, vendor));
        } else {
            // Perfil Normal de Navegação com Banda do Plano
            replies.addAll(buildRateLimitAttributes(username, downloadMbps, uploadMbps, vendor));

            // IP Fixo IPv4 (Opcional - IPAM)
            if (fixedIp != null && !fixedIp.isBlank()) {
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Framed-IP-Address")
                        .op("=")
                        .value(fixedIp)
                        .build());
            }

            // Prefixo IPv6 Delegado /56 ou /64 (Opcional - IPAM)
            if (ipv6Prefix != null && !ipv6Prefix.isBlank()) {
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Delegated-IPv6-Prefix")
                        .op("=")
                        .value(ipv6Prefix)
                        .build());
            }
        }

        // Atributo Padrão RFC (Framed-Protocol = PPP)
        replies.add(RadReply.builder()
                .username(username)
                .attribute("Framed-Protocol")
                .op("=")
                .value("PPP")
                .build());

        radReplyRepository.saveAll(replies);
        log.info("Assinante {} provisionado com sucesso com {} atributos RADIUS.", username, replies.size());
    }

    private List<RadReply> buildRateLimitAttributes(String username, long downloadMbps, long uploadMbps, NasVendorType vendor) {
        List<RadReply> replies = new ArrayList<>();

        long downloadBps = downloadMbps * 1_000_000L;
        long uploadBps = uploadMbps * 1_000_000L;

        switch (vendor) {
            case MIKROTIK -> {
                // Formato RouterOS: "uploadM/downloadM"
                String rateLimit = String.format("%dM/%dM", uploadMbps, downloadMbps);
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Mikrotik-Rate-Limit")
                        .op("=")
                        .value(rateLimit)
                        .build());
            }
            case HUAWEI -> {
                // Formato Huawei ME60 / NE40
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Huawei-Input-Average-Rate")
                        .op("=")
                        .value(String.valueOf(uploadBps))
                        .build());
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Huawei-Output-Average-Rate")
                        .op("=")
                        .value(String.valueOf(downloadBps))
                        .build());
            }
            case JUNIPER -> {
                // Formato Juniper ERX / MX
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("ERX-Ingress-Policy-Name")
                        .op("=")
                        .value(String.format("RATELIMIT-UP-%dM", uploadMbps))
                        .build());
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("ERX-Egress-Policy-Name")
                        .op("=")
                        .value(String.format("RATELIMIT-DOWN-%dM", downloadMbps))
                        .build());
            }
            case ACCEL_PPP, CISCO, A10, HILLSTONE, GENERIC -> {
                // Padrão Cisco / WISPr / RFC
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("WISPr-Bandwidth-Max-Down")
                        .op("=")
                        .value(String.valueOf(downloadBps))
                        .build());
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("WISPr-Bandwidth-Max-Up")
                        .op("=")
                        .value(String.valueOf(uploadBps))
                        .build());
            }
        }

        return replies;
    }

    private List<RadReply> buildBlockedAttributes(String username, NasVendorType vendor) {
        List<RadReply> replies = new ArrayList<>();

        switch (vendor) {
            case MIKROTIK -> {
                // Joga cliente na address-list de bloqueio do MikroTik com velocidade reduzida
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Mikrotik-Address-List")
                        .op("=")
                        .value("pg_bloqueados")
                        .build());
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Mikrotik-Rate-Limit")
                        .op("=")
                        .value("128k/128k")
                        .build());
            }
            case HUAWEI -> {
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Huawei-Domain-Name")
                        .op("=")
                        .value("bloqueio")
                        .build());
            }
            default -> {
                // Pool de bloqueio genérico
                replies.add(RadReply.builder()
                        .username(username)
                        .attribute("Framed-Pool")
                        .op("=")
                        .value("pool-bloqueio")
                        .build());
            }
        }

        return replies;
    }

    @Transactional
    public void deprovisionSubscriber(String username) {
        log.info("Removendo credenciais e atributos RADIUS de: {}", username);
        radCheckRepository.deleteByUsername(username);
        radReplyRepository.deleteByUsername(username);
    }
}
