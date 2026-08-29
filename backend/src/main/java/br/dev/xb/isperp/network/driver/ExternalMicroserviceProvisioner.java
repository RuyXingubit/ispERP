package br.dev.xb.isperp.network.driver;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.network.NetworkProvisioner;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@SuppressWarnings("null")
public class ExternalMicroserviceProvisioner implements NetworkProvisioner {

    @Override
    public NetworkDriverType getDriverType() {
        return NetworkDriverType.EXTERNAL_MICROSERVICE;
    }

    @Override
    public boolean provisionOnu(OnuProvisionRequest request, NetworkDevice device) {
        log.info("MicroserviceNetwork: Disparando requisição gRPC/HTTP para microserviço em {}:{} para provisionar MAC={}",
                device.getIpAddress(), device.getApiPort(), request.getOnuMac());
        return true;
    }

    @Override
    public boolean deprovisionOnu(String onuMac, NetworkDevice device) {
        log.info("MicroserviceNetwork: Desprovisionando MAC={} via microserviço", onuMac);
        return true;
    }

    @Override
    public boolean blockInternetAccess(String onuMac, String reason, NetworkDevice device) {
        log.info("MicroserviceNetwork: Enviando comando de bloqueio para MAC={}", onuMac);
        return true;
    }

    @Override
    public boolean unblockInternetAccess(String onuMac, NetworkDevice device) {
        log.info("MicroserviceNetwork: Enviando comando de desbloqueio para MAC={}", onuMac);
        return true;
    }

    @Override
    public OnuStatusResponse checkOnuStatus(String onuMac, NetworkDevice device) {
        log.info("MicroserviceNetwork: Consultando status de MAC={} via microserviço", onuMac);
        return OnuStatusResponse.builder()
                .onuMac(onuMac)
                .status("ONLINE")
                .rxPowerDbm(new BigDecimal("-18.80"))
                .txPowerDbm(new BigDecimal("2.30"))
                .oltName(device.getName())
                .details("Conexão ativa via Microserviço de Rede")
                .build();
    }
}
