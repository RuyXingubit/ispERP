package br.dev.xb.isperp.network;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.driver.SmartOltProvisioner;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NetworkDriverResolverTest {

    @Mock
    private NetworkDeviceRepository deviceRepository;

    private NetworkDriverResolver resolver;
    private SmartOltProvisioner smartOltProvisioner;
    private NetworkDevice sampleDevice;

    @BeforeEach
    void setUp() {
        smartOltProvisioner = new SmartOltProvisioner();
        resolver = new NetworkDriverResolver(List.of(smartOltProvisioner), deviceRepository);

        sampleDevice = NetworkDevice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("SmartOLT POP 01")
                .driverType(NetworkDriverType.SMARTOLT)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve resolver SmartOltProvisioner e dispositivo de rede ativo")
    void shouldResolveSmartOltDriver() {
        when(deviceRepository.findFirstByDriverTypeAndActiveTrue(NetworkDriverType.SMARTOLT))
                .thenReturn(Optional.of(sampleDevice));

        NetworkDriverResolver.ResolvedNetworkDriver resolved = resolver.resolve(NetworkDriverType.SMARTOLT);

        assertNotNull(resolved);
        assertEquals(NetworkDriverType.SMARTOLT, resolved.provisioner().getDriverType());
        assertEquals("SmartOLT POP 01", resolved.device().getName());
    }
}
