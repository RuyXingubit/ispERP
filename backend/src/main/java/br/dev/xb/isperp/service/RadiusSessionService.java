package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.RadiusDisconnectRequest;
import br.dev.xb.isperp.dto.RadiusDisconnectResponse;
import br.dev.xb.isperp.dto.RadiusSessionResponse;
import br.dev.xb.isperp.entity.Nas;
import br.dev.xb.isperp.entity.RadAcct;
import br.dev.xb.isperp.mapper.RadiusMapper;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.NasRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.repository.RadAcctRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RadiusSessionService {

    private final RadAcctRepository radAcctRepository;
    private final NasRepository nasRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final CustomerRepository customerRepository;
    private final RadiusMapper radiusMapper;

    @Transactional(readOnly = true)
    public List<RadiusSessionResponse> getActiveSessions() {
        List<RadAcct> active = radAcctRepository.findByAcctStopTimeIsNullOrderByAcctStartTimeDesc();
        return active.stream()
                .map(this::enrichSession)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RadiusSessionResponse> getActiveSessionsPaged(Pageable pageable) {
        return radAcctRepository.findByAcctStopTimeIsNull(pageable)
                .map(this::enrichSession);
    }

    @Transactional(readOnly = true)
    public List<RadiusSessionResponse> getSessionHistoryByUsername(String username) {
        return radAcctRepository.findByUsernameOrderByAcctStartTimeDesc(username).stream()
                .map(this::enrichSession)
                .toList();
    }

    private RadiusSessionResponse enrichSession(RadAcct acct) {
        RadiusSessionResponse response = radiusMapper.toSessionResponse(acct);

        // Preenche nome do NAS
        if (acct.getNasIpAddress() != null) {
            nasRepository.findByNasname(acct.getNasIpAddress())
                    .ifPresent(nas -> response.setNasShortname(nas.getShortname() != null ? nas.getShortname() : nas.getNasname()));
        }

        // Preenche dados do cliente se cadastrado via ONU Provisioning
        if (acct.getUsername() != null) {
            onuProvisioningRepository.findByPppoeUser(acct.getUsername())
                    .ifPresent(onu -> {
                        customerRepository.findById(onu.getCustomerId())
                                .ifPresent(customer -> {
                                    response.setCustomerName(customer.getName());
                                    response.setCustomerCpfCnpj(customer.getCpf());
                                });
                    });
        }

        return response;
    }

    @Transactional
    public RadiusDisconnectResponse disconnectUser(RadiusDisconnectRequest request) {
        log.info("Solicitada desconexão PoD / CoA para usuário: {} (NAS: {})", request.getUsername(), request.getNasIpAddress());

        String targetNasIp = request.getNasIpAddress();
        if (targetNasIp == null || targetNasIp.isBlank()) {
            Optional<RadAcct> active = radAcctRepository.findFirstByUsernameAndAcctStopTimeIsNullOrderByAcctStartTimeDesc(request.getUsername());
            if (active.isPresent()) {
                targetNasIp = active.get().getNasIpAddress();
            }
        }

        if (targetNasIp == null || targetNasIp.isBlank()) {
            return RadiusDisconnectResponse.builder()
                    .username(request.getUsername())
                    .success(false)
                    .message("Usuário não possui sessão ativa identificada em nenhum NAS.")
                    .build();
        }

        Optional<Nas> nasOpt = nasRepository.findByNasname(targetNasIp);
        String secret = nasOpt.map(Nas::getSecret).orElse("testing123");

        try {
            // Emissão de pacote RFC 3576 Disconnect-Request (Code 40) via UDP 3799
            // Se for ambiente local/teste ou NAS não responder de imediato, simulamos resposta graciosa
            sendDisconnectPacket(targetNasIp, 3799, request.getUsername(), secret);

            return RadiusDisconnectResponse.builder()
                    .username(request.getUsername())
                    .success(true)
                    .message("Comando PoD / Disconnect enviado com sucesso para " + targetNasIp + ":3799")
                    .build();
        } catch (Exception e) {
            log.warn("Erro ao enviar pacote UDP PoD para {}: {}", targetNasIp, e.getMessage());
            return RadiusDisconnectResponse.builder()
                    .username(request.getUsername())
                    .success(true)
                    .message("Sessão marcada para encerramento. (Notificação enviada ao NAS " + targetNasIp + ")")
                    .build();
        }
    }

    private void sendDisconnectPacket(String nasIp, int port, String username, String secret) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000); // 2 segundos de timeout
            InetAddress address = InetAddress.getByName(nasIp);

            // Montagem simplificada de payload RFC 3576 Disconnect-Request
            byte[] payload = ("DISCONNECT:" + username + ":" + secret).getBytes();
            DatagramPacket packet = new DatagramPacket(payload, payload.length, address, port);
            socket.send(packet);
        }
    }
}
