package br.dev.xb.isperp.network.driver;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.network.NetworkProvisioner;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@SuppressWarnings("null")
public class MockNetworkProvisioner implements NetworkProvisioner {

    @Override
    public NetworkDriverType getDriverType() {
        return NetworkDriverType.MOCK;
    }

    @Override
    public boolean provisionOnu(OnuProvisionRequest request, NetworkDevice device) {
        log.info("MockNetwork: Simulando provisionamento de ONU MAC={}", request.getOnuMac());
        return true;
    }

    @Override
    public boolean deprovisionOnu(String onuMac, NetworkDevice device) {
        log.info("MockNetwork: Simulando desprovisionamento de ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public boolean blockInternetAccess(String onuMac, String reason, NetworkDevice device) {
        log.info("MockNetwork: Simulando bloqueio de ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public boolean unblockInternetAccess(String onuMac, NetworkDevice device) {
        log.info("MockNetwork: Simulando desbloqueio de ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public OnuStatusResponse checkOnuStatus(String onuMac, NetworkDevice device) {
        return NetworkProvisioner.buildOnlineStatus(
                onuMac, "-20.00", "2.00", "Mock OLT", "Simulação Mock de Sinal"
        );
    }
}
