package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.mapper.NetworkDeviceMapperImpl;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkDeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(NetworkDeviceMapperImpl.class)
class NetworkDeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NetworkDeviceRepository deviceRepository;

    @Test
    @DisplayName("GET /network-devices - Deve listar dispositivos sem vazar credenciais privadas")
    void testGetAllDevices() throws Exception {
        UUID deviceId = UuidCreatorUtils.generateUuidV7();
        NetworkDevice device = NetworkDevice.builder()
                .id(deviceId)
                .name("OLT Central Huawei")
                .deviceType("OLT")
                .driverType(NetworkDriverType.SMARTOLT)
                .ipAddress("192.168.1.10")
                .apiPort(443)
                .apiToken("SUPER_SECRET_OLT_API_KEY_123")
                .snmpCommunity("private_snmp_community")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findAll()).thenReturn(List.of(device));

        mockMvc.perform(get("/network-devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deviceId.toString()))
                .andExpect(jsonPath("$[0].name").value("OLT Central Huawei"))
                .andExpect(jsonPath("$[0].driverType").value("SMARTOLT"))
                .andExpect(jsonPath("$[0].ipAddress").value("192.168.1.10"))
                // Asserções estritas de segurança: Segredos NUNCA devem ser serializados
                .andExpect(jsonPath("$[0].apiToken").doesNotExist())
                .andExpect(jsonPath("$[0].snmpCommunity").doesNotExist());
    }

    @Test
    @DisplayName("GET /network-devices/{id} - Deve retornar dispositivo por ID")
    void testGetDeviceById() throws Exception {
        UUID deviceId = UuidCreatorUtils.generateUuidV7();
        NetworkDevice device = NetworkDevice.builder()
                .id(deviceId)
                .name("OLT Bairro Sul ZTE")
                .deviceType("OLT")
                .driverType(NetworkDriverType.SMARTOLT)
                .ipAddress("192.168.2.10")
                .active(true)
                .apiToken("SECRET_TOKEN")
                .snmpCommunity("community_v2")
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        mockMvc.perform(get("/network-devices/{id}", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deviceId.toString()))
                .andExpect(jsonPath("$.name").value("OLT Bairro Sul ZTE"))
                .andExpect(jsonPath("$.apiToken").doesNotExist())
                .andExpect(jsonPath("$.snmpCommunity").doesNotExist());
    }

    @Test
    @DisplayName("GET /network-devices/{id} - Deve retornar 404 para ID inexistente")
    void testGetDeviceByIdNotFound() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/network-devices/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /network-devices - Deve cadastrar dispositivo com sucesso")
    void testSaveDevice() throws Exception {
        UUID deviceId = UuidCreatorUtils.generateUuidV7();
        NetworkDevice saved = NetworkDevice.builder()
                .id(deviceId)
                .name("OLT Nova ZTE C320")
                .deviceType("OLT")
                .driverType(NetworkDriverType.SMARTOLT)
                .ipAddress("10.10.1.1")
                .apiPort(443)
                .apiToken("private_key")
                .snmpCommunity("private")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceRepository.save(any(NetworkDevice.class))).thenReturn(saved);

        String json = """
                {
                    "name": "OLT Nova ZTE C320",
                    "deviceType": "OLT",
                    "driverType": "SMARTOLT",
                    "ipAddress": "10.10.1.1",
                    "apiPort": 443,
                    "apiToken": "private_key",
                    "snmpCommunity": "private",
                    "active": true
                }
                """;

        mockMvc.perform(post("/network-devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(deviceId.toString()))
                .andExpect(jsonPath("$.name").value("OLT Nova ZTE C320"))
                .andExpect(jsonPath("$.apiToken").doesNotExist())
                .andExpect(jsonPath("$.snmpCommunity").doesNotExist());
    }
}
