package br.dev.xb.isperp.network;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class NetworkDriverResolver {

    private final List<NetworkProvisioner> provisioners;
    private final NetworkDeviceRepository deviceRepository;

    /**
     * Resolve o driver de rede e o dispositivo de destino adequado.
     *
     * @param preferredType Tipo de driver preferido
     * @return Par contendo o Driver e o Dispositivo de Rede (OLT)
     */
    public ResolvedNetworkDriver resolve(NetworkDriverType preferredType) {
        NetworkDriverType targetType = (preferredType != null) ? preferredType : NetworkDriverType.SMARTOLT;

        NetworkProvisioner provisioner = provisioners.stream()
                .filter(p -> p.getDriverType() == targetType)
                .findFirst()
                .orElseGet(() -> provisioners.stream()
                        .filter(p -> p.getDriverType() == NetworkDriverType.SMARTOLT)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Nenhum driver de rede disponível")));

        Optional<NetworkDevice> deviceOpt = deviceRepository.findFirstByDriverTypeAndActiveTrue(provisioner.getDriverType());
        NetworkDevice device = deviceOpt.orElseGet(() ->
                deviceRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                        .orElseGet(() -> NetworkDevice.builder()
                                .name("SmartOLT Default")
                                .deviceType("OLT")
                                .driverType(NetworkDriverType.SMARTOLT)
                                .ipAddress("10.0.0.1")
                                .apiPort(443)
                                .apiToken("token_demo")
                                .active(true)
                                .build()
                        )
        );

        return new ResolvedNetworkDriver(provisioner, device);
    }

    public record ResolvedNetworkDriver(NetworkProvisioner provisioner, NetworkDevice device) {}
}
