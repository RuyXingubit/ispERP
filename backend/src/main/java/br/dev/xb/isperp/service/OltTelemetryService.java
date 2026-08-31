package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.FtthMonitoringMapper;
import br.dev.xb.isperp.monitoring.OnuSignalStatus;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class OltTelemetryService {

    private final NetworkDeviceRepository networkDeviceRepository;
    private final OltPonPortRepository oltPonPortRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final OnuTelemetryRecordRepository onuTelemetryRecordRepository;
    private final FtthCableRepository cableRepository;
    private final FtthMonitoringMapper mapper;

    // Rate Limiter por OLT: no máximo 3 operações simultâneas por dispositivo para não sobrecarregar CPU da MPU/OLT
    private final Map<UUID, Semaphore> oltSemaphores = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<OltPonPortResponse> getPonPortsByDevice(UUID networkDeviceId) {
        return oltPonPortRepository.findByNetworkDeviceIdOrderBySlotNumberAscPortNumberAsc(networkDeviceId).stream()
                .map(this::enrichPonPortResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OltPonPortResponse> getAllPonPorts() {
        return oltPonPortRepository.findAll().stream()
                .map(this::enrichPonPortResponse)
                .toList();
    }

    @Transactional
    public OltPonPortResponse createPonPort(OltPonPortRequest request) {
        OltPonPort port = mapper.toPonPortEntity(request);
        port = oltPonPortRepository.save(port);
        return enrichPonPortResponse(port);
    }

    /**
     * CAMADA 2: Polling Leve de Resumo das Portas PON (Summary Polling).
     * Roda rapidamente consultando apenas o resumo de cada PON sem varrer cliente por cliente.
     */
    @Transactional
    public void pollOltPonSummaries() {
        List<NetworkDevice> olts = networkDeviceRepository.findAll().stream()
                .filter(d -> "OLT".equalsIgnoreCase(d.getDeviceType()) && Boolean.TRUE.equals(d.getActive()))
                .toList();

        for (NetworkDevice olt : olts) {
            Semaphore sem = oltSemaphores.computeIfAbsent(olt.getId(), k -> new Semaphore(3));
            if (!sem.tryAcquire()) {
                log.warn("OLT {} ocupada em outro ciclo de telemetria, pulando ciclo atual.", olt.getName());
                continue;
            }

            try {
                List<OltPonPort> ports = oltPonPortRepository.findByNetworkDeviceIdOrderBySlotNumberAscPortNumberAsc(olt.getId());
                for (OltPonPort port : ports) {
                    updatePonPortSummaryFromOnus(port);
                }
            } finally {
                sem.release();
            }
        }
    }

    /**
     * Atualiza os contadores agregados da PON a partir das ONUs provisionadas.
     */
    @Transactional
    public void updatePonPortSummaryFromOnus(OltPonPort port) {
        List<OnuProvisioning> onus = onuProvisioningRepository.findByNetworkDeviceId(port.getNetworkDeviceId());
        int total = onus.size();
        int online = 0;
        int los = 0;
        int dyingGasp = 0;
        int offline = 0;

        for (OnuProvisioning onu : onus) {
            if (onu.getRxPowerDbm() == null) {
                offline++;
            } else if (onu.getRxPowerDbm().compareTo(new BigDecimal("-40.00")) <= 0) {
                los++;
            } else {
                online++;
            }
        }

        port.setTotalOnus(total);
        port.setOnlineOnus(online);
        port.setLosOnus(los);
        port.setDyingGaspOnus(dyingGasp);
        port.setOfflineOnus(offline);
        port.setOperStatus(los > 0 && online == 0 && total > 0 ? "FAULT" : (los > 0 ? "DEGRADED" : "ACTIVE"));
        port.setLastPolledAt(OffsetDateTime.now());
        oltPonPortRepository.save(port);
    }

    /**
     * CAMADA 3: Atualização de Telemetria de uma ONU (Rx/Tx dBm, Status).
     */
    @Transactional
    public void recordOnuTelemetry(UUID onuProvisioningId, BigDecimal rxPowerDbm, BigDecimal txPowerDbm, OnuSignalStatus signalStatus, int distanceMeters) {
        OnuProvisioning onu = onuProvisioningRepository.findById(onuProvisioningId)
                .orElseThrow(() -> new IllegalArgumentException("ONU não encontrada: " + onuProvisioningId));

        onu.setRxPowerDbm(rxPowerDbm);
        onuProvisioningRepository.save(onu);

        OnuTelemetryRecord record = OnuTelemetryRecord.builder()
                .companyId(null)
                .onuProvisioningId(onuProvisioningId)
                .rxPowerDbm(rxPowerDbm)
                .txPowerDbm(txPowerDbm)
                .signalStatus(signalStatus)
                .distanceMeters(distanceMeters)
                .build();
        onuTelemetryRecordRepository.save(record);
    }

    private OltPonPortResponse enrichPonPortResponse(OltPonPort port) {
        OltPonPortResponse resp = mapper.toPonPortResponse(port);
        networkDeviceRepository.findById(port.getNetworkDeviceId()).ifPresent(d -> resp.setOltName(d.getName()));
        if (port.getConnectedCableId() != null) {
            cableRepository.findById(port.getConnectedCableId()).ifPresent(c -> resp.setConnectedCableName(c.getName()));
        }
        double health = port.getTotalOnus() > 0 ? ((double) port.getOnlineOnus() / port.getTotalOnus()) * 100.0 : 100.0;
        resp.setHealthPercentage(BigDecimal.valueOf(health).setScale(1, RoundingMode.HALF_UP).doubleValue());
        return resp;
    }
}
