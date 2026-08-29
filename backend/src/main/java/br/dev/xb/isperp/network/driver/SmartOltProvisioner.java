package br.dev.xb.isperp.network.driver;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.network.NetworkProvisioner;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@SuppressWarnings("null")
public class SmartOltProvisioner implements NetworkProvisioner {

    @Override
    public NetworkDriverType getDriverType() {
        return NetworkDriverType.SMARTOLT;
    }

    @Override
    public boolean provisionOnu(@NonNull OnuProvisionRequest request, @NonNull NetworkDevice device) {
        log.info("SmartOLT: Autorizando ONU MAC={} (SN={}) na OLT '{}' ({}) com perfil de download={}M/upload={}M",
                request.getOnuMac(), request.getOnuSerial(), device.getName(), device.getIpAddress(),
                request.getDownloadSpeed(), request.getUploadSpeed());
        // Simula chamada à API REST da SmartOLT
        return true;
    }

    @Override
    public boolean deprovisionOnu(@NonNull String onuMac, @NonNull NetworkDevice device) {
        log.info("SmartOLT: Desprovisionando ONU MAC={} da OLT '{}'", onuMac, device.getName());
        return true;
    }

    @Override
    public boolean blockInternetAccess(@NonNull String onuMac, String reason, @NonNull NetworkDevice device) {
        log.info("SmartOLT: Aplicando bloqueio de tráfego na ONU MAC={} por motivo: {}", onuMac, reason);
        return true;
    }

    @Override
    public boolean unblockInternetAccess(@NonNull String onuMac, @NonNull NetworkDevice device) {
        log.info("SmartOLT: Removendo bloqueio e restaurando tráfego total para ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public OnuStatusResponse checkOnuStatus(@NonNull String onuMac, @NonNull NetworkDevice device) {
        log.info("SmartOLT: Diagnosticando sinal óptico para ONU MAC={}", onuMac);
        return OnuStatusResponse.builder()
                .onuMac(onuMac)
                .status("ONLINE")
                .rxPowerDbm(new BigDecimal("-19.45"))
                .txPowerDbm(new BigDecimal("2.10"))
                .oltName(device.getName())
                .ponPort(1)
                .details("Sinal óptico excelente (-19.45 dBm). PON Slot 1/1/1")
                .build();
    }
}
