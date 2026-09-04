package br.dev.xb.isperp.network.driver;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class SmartOltProvisionerTest {

    private SmartOltProvisioner provisioner;
    private NetworkDevice sampleDevice;

    @BeforeEach
    void setUp() {
        provisioner = new SmartOltProvisioner();
        sampleDevice = NetworkDevice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("SmartOLT Central")
                .driverType(NetworkDriverType.SMARTOLT)
                .ipAddress("10.0.0.1")
                .build();
    }

    @Test
    @DisplayName("Deve autorizar e provisionar ONU na OLT")
    void shouldProvisionOnu() {
        OnuProvisionRequest req = OnuProvisionRequest.builder()
                .onuMac("AA:BB:CC:DD:EE:01")
                .onuSerial("HWTC12345678")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .build();

        boolean success = provisioner.provisionOnu(req, sampleDevice);
        assertTrue(success);
    }

    @Test
    @DisplayName("Deve consultar status da ONU sem inventar potências ópticas")
    void shouldCheckOnuStatus() {
        OnuStatusResponse response = provisioner.checkOnuStatus("AA:BB:CC:DD:EE:01", sampleDevice);

        assertNotNull(response);
        assertEquals("UNKNOWN", response.getStatus());
        assertNull(response.getRxPowerDbm());
        assertNull(response.getTxPowerDbm());
        assertEquals("SmartOLT Central", response.getOltName());
    }
}
