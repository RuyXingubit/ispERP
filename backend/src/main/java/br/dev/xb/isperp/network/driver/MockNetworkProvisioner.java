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
public class MockNetworkProvisioner implements NetworkProvisioner {

    @Override
    public NetworkDriverType getDriverType() {
        return NetworkDriverType.MOCK;
    }

    @Override
    public boolean provisionOnu(@NonNull OnuProvisionRequest request, @NonNull NetworkDevice device) {
        log.info("MockNetwork: Simulando provisionamento de ONU MAC={}", request.getOnuMac());
        return true;
    }

    @Override
    public boolean deprovisionOnu(@NonNull String onuMac, @NonNull NetworkDevice device) {
        log.info("MockNetwork: Simulando desprovisionamento de ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public boolean blockInternetAccess(@NonNull String onuMac, String reason, @NonNull NetworkDevice device) {
        log.info("MockNetwork: Simulando bloqueio de ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public boolean unblockInternetAccess(@NonNull String onuMac, @NonNull NetworkDevice device) {
        log.info("MockNetwork: Simulando desbloqueio de ONU MAC={}", onuMac);
        return true;
    }

    @Override
    public OnuStatusResponse checkOnuStatus(@NonNull String onuMac, @NonNull NetworkDevice device) {
        return OnuStatusResponse.builder()
                .onuMac(onuMac)
                .status("ONLINE")
                .rxPowerDbm(new BigDecimal("-20.00"))
                .txPowerDbm(new BigDecimal("2.00"))
                .oltName("Mock OLT")
                .details("Simulação Mock de Sinal")
                .build();
    }
}
