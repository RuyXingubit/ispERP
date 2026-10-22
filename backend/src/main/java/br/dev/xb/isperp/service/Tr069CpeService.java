package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.Tr069CommandLogResponse;
import br.dev.xb.isperp.api.dto.Tr069CpeStatusResponse;
import br.dev.xb.isperp.api.dto.UpdateTr069WifiRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.entity.Tr069CommandLog;
import br.dev.xb.isperp.entity.Tr069CpeConfig;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.repository.Tr069CommandLogRepository;
import br.dev.xb.isperp.repository.Tr069CpeConfigRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null", "java:S2583"})
public class Tr069CpeService {

    private final Tr069CpeConfigRepository cpeConfigRepository;
    private final Tr069CommandLogRepository commandLogRepository;
    private final ContractRepository contractRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;

    @Value("${isperp.tr069.genieacs-url:}")
    private String genieAcsUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    @Transactional
    public Tr069CpeStatusResponse getCpeStatusByContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + contractId));
        Tr069CpeConfig config = getOrCreateConfig(contract);
        return toStatusResponse(config);
    }

    @Transactional
    public Tr069CpeStatusResponse getCpeStatusByCustomer(UUID customerId) {
        Contract contract = resolvePrimaryContractForCustomer(customerId);
        Tr069CpeConfig config = getOrCreateConfig(contract);
        return toStatusResponse(config);
    }

    @Transactional
    public Tr069CpeStatusResponse updateWifiByContract(
            UUID contractId,
            UpdateTr069WifiRequest request,
            String originChannel
    ) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + contractId));

        validateWifiRequest(request);

        Tr069CpeConfig config = getOrCreateConfig(contract);
        config.setSsid24Ghz(request.getSsid24Ghz().trim());
        config.setSsid5Ghz(request.getSsid5Ghz().trim());

        if (request.getPassword24Ghz() != null && !request.getPassword24Ghz().isBlank()) {
            config.setPassword24Ghz(request.getPassword24Ghz().trim());
        }
        if (request.getPassword5Ghz() != null && !request.getPassword5Ghz().isBlank()) {
            config.setPassword5Ghz(request.getPassword5Ghz().trim());
        }
        if (request.getChannel24Ghz() != null && !request.getChannel24Ghz().isBlank()) {
            config.setChannel24Ghz(request.getChannel24Ghz().trim().toUpperCase());
        }
        if (request.getChannel5Ghz() != null && !request.getChannel5Ghz().isBlank()) {
            config.setChannel5Ghz(request.getChannel5Ghz().trim().toUpperCase());
        }
        if (request.getBandSteeringEnabled() != null) {
            config.setBandSteeringEnabled(request.getBandSteeringEnabled());
        }

        boolean pushedToGenieAcs = dispatchSetParameterValuesToGenieAcs(config);
        config.setAcsSyncStatus(pushedToGenieAcs ? "ONLINE_GENIEACS" : "SYNCED_EMBEDDED_ACS");
        config.setLastInformAt(OffsetDateTime.now());
        config = cpeConfigRepository.save(config);

        String requestedBy = (request.getRequestedBy() != null && !request.getRequestedBy().isBlank())
                ? request.getRequestedBy().trim()
                : ("SUBSCRIBER_PORTAL".equalsIgnoreCase(originChannel) ? "Assinante (Autoatendimento)" : "Suporte Técnico");

        Tr069CommandLog cmdLog = Tr069CommandLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contract.getId())
                .onuSerial(config.getOnuSerial())
                .commandType("SET_WIFI_PARAMS")
                .originChannel(originChannel)
                .requestedBy(requestedBy)
                .status(pushedToGenieAcs ? "APPLIED_ACS" : "APPLIED_LOCAL_SYNC")
                .summaryMessage(String.format(
                        "Wi-Fi atualizado via TR-069 (2.4G: '%s' [Canal %s] / 5G: '%s' [Canal %s])",
                        config.getSsid24Ghz(), config.getChannel24Ghz(),
                        config.getSsid5Ghz(), config.getChannel5Ghz()
                ))
                .createdAt(OffsetDateTime.now())
                .build();
        commandLogRepository.save(cmdLog);

        return toStatusResponse(config);
    }

    @Transactional
    public Tr069CpeStatusResponse updateWifiByCustomer(UUID customerId, UpdateTr069WifiRequest request) {
        Contract contract = resolvePrimaryContractForCustomer(customerId);
        return updateWifiByContract(contract.getId(), request, "SUBSCRIBER_PORTAL");
    }

    @Transactional
    public Tr069CommandLogResponse rebootCpeByContract(UUID contractId, String originChannel, String requestedBy) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + contractId));

        Tr069CpeConfig config = getOrCreateConfig(contract);
        boolean pushedToGenieAcs = dispatchRebootToGenieAcs(config);

        OffsetDateTime now = OffsetDateTime.now();
        config.setLastRebootAt(now);
        config.setLastInformAt(now);
        config.setUptimeSeconds(0L);
        config.setAcsSyncStatus(pushedToGenieAcs ? "ONLINE_GENIEACS" : "SYNCED_EMBEDDED_ACS");
        cpeConfigRepository.save(config);

        Tr069CommandLog cmdLog = Tr069CommandLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contract.getId())
                .onuSerial(config.getOnuSerial())
                .commandType("REBOOT_CPE")
                .originChannel(originChannel)
                .requestedBy(requestedBy != null && !requestedBy.isBlank() ? requestedBy : "Operador / Assinante")
                .status(pushedToGenieAcs ? "APPLIED_ACS" : "APPLIED_LOCAL_SYNC")
                .summaryMessage("Comando TR-069 Reboot enviado para a ONU/CPE " + (config.getOnuSerial() != null ? config.getOnuSerial() : ""))
                .createdAt(now)
                .build();
        cmdLog = commandLogRepository.save(cmdLog);

        return toCommandLogResponse(cmdLog);
    }

    @Transactional
    public Tr069CommandLogResponse rebootCpeByCustomer(UUID customerId) {
        Contract contract = resolvePrimaryContractForCustomer(customerId);
        return rebootCpeByContract(contract.getId(), "SUBSCRIBER_PORTAL", "Assinante (Central do Cliente)");
    }

    private void validateWifiRequest(@org.jspecify.annotations.Nullable UpdateTr069WifiRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload de configuração Wi-Fi não informado.");
        }
        if (request.getSsid24Ghz() == null || request.getSsid24Ghz().trim().length() < 2 || request.getSsid24Ghz().trim().length() > 32) {
            throw new IllegalArgumentException("O nome da rede Wi-Fi 2.4 GHz (SSID) deve ter entre 2 e 32 caracteres.");
        }
        if (request.getSsid5Ghz() == null || request.getSsid5Ghz().trim().length() < 2 || request.getSsid5Ghz().trim().length() > 32) {
            throw new IllegalArgumentException("O nome da rede Wi-Fi 5 GHz (SSID) deve ter entre 2 e 32 caracteres.");
        }
        if (request.getPassword24Ghz() != null && !request.getPassword24Ghz().isBlank()) {
            String pwd24 = request.getPassword24Ghz().trim();
            if (pwd24.length() < 8 || pwd24.length() > 63) {
                throw new IllegalArgumentException("A senha WPA2/WPA3 do Wi-Fi 2.4 GHz deve ter entre 8 e 63 caracteres.");
            }
        }
        if (request.getPassword5Ghz() != null && !request.getPassword5Ghz().isBlank()) {
            String pwd5 = request.getPassword5Ghz().trim();
            if (pwd5.length() < 8 || pwd5.length() > 63) {
                throw new IllegalArgumentException("A senha WPA2/WPA3 do Wi-Fi 5 GHz deve ter entre 8 e 63 caracteres.");
            }
        }
    }

    private Contract resolvePrimaryContractForCustomer(UUID customerId) {
        List<Contract> contracts = contractRepository.findByCustomerId(customerId);
        if (contracts.isEmpty()) {
            throw new ResourceNotFoundException("Nenhum contrato encontrado para o cliente: " + customerId);
        }
        return contracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE || c.getStatus() == Contract.ContractStatus.SUSPENDED)
                .findFirst()
                .orElse(contracts.get(0));
    }

    private Tr069CpeConfig getOrCreateConfig(Contract contract) {
        Optional<Tr069CpeConfig> existing = cpeConfigRepository.findByContractId(contract.getId());
        Optional<OnuProvisioning> onuOpt = onuProvisioningRepository.findByContractId(contract.getId());

        if (existing.isPresent()) {
            Tr069CpeConfig cfg = existing.get();
            if (onuOpt.isPresent() && cfg.getOnuId() == null) {
                OnuProvisioning onu = onuOpt.get();
                cfg.setOnuId(onu.getId());
                cfg.setOnuSerial(onu.getOnuMac());
                cfg = cpeConfigRepository.save(cfg);
            }
            return cfg;
        }

        String suffix = contract.getContractNumber() != null && contract.getContractNumber().length() >= 4
                ? contract.getContractNumber().substring(contract.getContractNumber().length() - 4)
                : contract.getId().toString().substring(0, 4).toUpperCase();

        OnuProvisioning onu = onuOpt.orElse(null);
        String serial = onu != null && onu.getOnuMac() != null ? onu.getOnuMac() : ("ONT-" + suffix);

        Tr069CpeConfig created = Tr069CpeConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contract.getId())
                .onuId(onu != null ? onu.getId() : null)
                .onuSerial(serial)
                .cpeModel("ONT Dual-Band Wi-Fi 6 Gigabit")
                .firmwareVersion("V5R020C00S280")
                .uptimeSeconds(86400L)
                .ssid24Ghz("XinguFibra_" + suffix + "_2G")
                .password24Ghz("xingufibra" + suffix.toLowerCase())
                .channel24Ghz("AUTO")
                .ssid5Ghz("XinguFibra_" + suffix + "_5G")
                .password5Ghz("xingufibra" + suffix.toLowerCase())
                .channel5Ghz("AUTO")
                .bandSteeringEnabled(true)
                .securityMode("WPA2-PSK/WPA3-SAE")
                .acsSyncStatus(isGenieAcsConfigured() ? "ONLINE_GENIEACS" : "SYNCED_EMBEDDED_ACS")
                .lastInformAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return cpeConfigRepository.save(created);
    }

    private boolean isGenieAcsConfigured() {
        return genieAcsUrl != null && !genieAcsUrl.isBlank();
    }

    void setGenieAcsUrl(String genieAcsUrl) {
        this.genieAcsUrl = genieAcsUrl;
    }

    private boolean dispatchSetParameterValuesToGenieAcs(Tr069CpeConfig config) {
        String payload = String.format(
                "{\"name\":\"setParameterValues\",\"parameterValues\":[" +
                "[\"InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.SSID\",\"%s\",\"xsd:string\"]," +
                "[\"InternetGatewayDevice.LANDevice.1.WLANConfiguration.1.KeyPassphrase\",\"%s\",\"xsd:string\"]," +
                "[\"InternetGatewayDevice.LANDevice.1.WLANConfiguration.5.SSID\",\"%s\",\"xsd:string\"]," +
                "[\"InternetGatewayDevice.LANDevice.1.WLANConfiguration.5.KeyPassphrase\",\"%s\",\"xsd:string\"]" +
                "]}",
                config.getSsid24Ghz(), config.getPassword24Ghz(),
                config.getSsid5Ghz(), config.getPassword5Ghz()
        );
        return dispatchTaskToGenieAcs(config, payload);
    }

    private boolean dispatchRebootToGenieAcs(Tr069CpeConfig config) {
        return dispatchTaskToGenieAcs(config, "{\"name\":\"reboot\"}");
    }

    private boolean dispatchTaskToGenieAcs(Tr069CpeConfig config, String payload) {
        String serial = config.getOnuSerial();
        if (!isGenieAcsConfigured() || serial == null || serial.isBlank()) {
            return false;
        }
        try {
            String deviceId = URLEncoder.encode(serial, StandardCharsets.UTF_8);
            String url = stripTrailingSlashes(genieAcsUrl) + "/devices/" + deviceId + "/tasks?connection_request";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("GenieACS NBI interrompido ({}). Mantendo sincronização embarcada no PostgreSQL.", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("GenieACS NBI indisponível ({}). Mantendo sincronização embarcada no PostgreSQL.", e.getMessage());
            return false;
        }
    }

    static String stripTrailingSlashes(String input) {
        int end = input.length();
        while (end > 0 && input.charAt(end - 1) == '/') {
            end--;
        }
        return input.substring(0, end);
    }

    private Tr069CpeStatusResponse toStatusResponse(Tr069CpeConfig config) {
        Tr069CpeStatusResponse res = new Tr069CpeStatusResponse();
        res.setId(config.getId());
        res.setContractId(config.getContractId());
        res.setOnuId(config.getOnuId());
        res.setOnuSerial(config.getOnuSerial());
        res.setCpeModel(config.getCpeModel());
        res.setFirmwareVersion(config.getFirmwareVersion());
        res.setUptimeSeconds(config.getUptimeSeconds());
        res.setSsid24Ghz(config.getSsid24Ghz());
        res.setPassword24GhzMasked(maskPassword(config.getPassword24Ghz()));
        res.setChannel24Ghz(config.getChannel24Ghz());
        res.setSsid5Ghz(config.getSsid5Ghz());
        res.setPassword5GhzMasked(maskPassword(config.getPassword5Ghz()));
        res.setChannel5Ghz(config.getChannel5Ghz());
        res.setBandSteeringEnabled(config.isBandSteeringEnabled());
        res.setSecurityMode(config.getSecurityMode());
        res.setAcsSyncStatus(config.getAcsSyncStatus());
        res.setLastInformAt(config.getLastInformAt());
        res.setLastRebootAt(config.getLastRebootAt());
        res.setConnectedHosts(Collections.emptyList());

        List<Tr069CommandLogResponse> recentLogs = commandLogRepository
                .findTop15ByContractIdOrderByCreatedAtDesc(config.getContractId())
                .stream()
                .map(this::toCommandLogResponse)
                .collect(Collectors.toList());
        res.setRecentCommands(recentLogs);

        return res;
    }

    private Tr069CommandLogResponse toCommandLogResponse(Tr069CommandLog logEntity) {
        Tr069CommandLogResponse dto = new Tr069CommandLogResponse();
        dto.setId(logEntity.getId());
        dto.setContractId(logEntity.getContractId());
        dto.setOnuSerial(logEntity.getOnuSerial());
        dto.setCommandType(logEntity.getCommandType());
        dto.setOriginChannel(logEntity.getOriginChannel());
        dto.setRequestedBy(logEntity.getRequestedBy());
        dto.setStatus(logEntity.getStatus());
        dto.setSummaryMessage(logEntity.getSummaryMessage());
        dto.setCreatedAt(logEntity.getCreatedAt());
        return dto;
    }

    private String maskPassword(String pwd) {
        if (pwd == null || pwd.length() < 4) return "••••••••";
        return pwd.substring(0, 2) + "••••••" + pwd.substring(pwd.length() - 2);
    }
}
