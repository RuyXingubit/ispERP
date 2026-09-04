package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.mapper.OnuMapperImpl;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import br.dev.xb.isperp.service.NetworkProvisioningService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OnuProvisioningController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OnuMapperImpl.class)
class OnuProvisioningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NetworkProvisioningService provisioningService;

    @Test
    @DisplayName("GET /onus - Deve listar todas as ONUs sem vazar pppoePassword")
    void testGetAllOnus() throws Exception {
        UUID onuId = UuidCreatorUtils.generateUuidV7();
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        OnuProvisioning onu = OnuProvisioning.builder()
                .id(onuId)
                .contractId(contractId)
                .customerId(customerId)
                .onuMac("48:57:02:1A:BC:DE")
                .onuSerial("HWTC1A2B3C4D")
                .vlanId(100)
                .pppoeUser("cliente.teste")
                .pppoePassword("SENHA_SECRETA_PPPOE") // Deve ser estritamente omitido
                .downloadSpeed(500)
                .uploadSpeed(250)
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .rxPowerDbm(new BigDecimal("-19.50"))
                .createdAt(LocalDateTime.now())
                .build();

        when(provisioningService.getAllProvisionings()).thenReturn(List.of(onu));

        mockMvc.perform(get("/onus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(onuId.toString()))
                .andExpect(jsonPath("$[0].onuMac").value("48:57:02:1A:BC:DE"))
                .andExpect(jsonPath("$[0].onuSerial").value("HWTC1A2B3C4D"))
                .andExpect(jsonPath("$[0].pppoeUser").value("cliente.teste"))
                // Asserção estrita de segurança: Senha do assinante não pode vazar
                .andExpect(jsonPath("$[0].pppoePassword").doesNotExist());
    }

    @Test
    @DisplayName("GET /onus/{id} - Deve retornar ONU por ID")
    void testGetOnuById() throws Exception {
        UUID onuId = UuidCreatorUtils.generateUuidV7();
        OnuProvisioning onu = OnuProvisioning.builder()
                .id(onuId)
                .contractId(UuidCreatorUtils.generateUuidV7())
                .customerId(UuidCreatorUtils.generateUuidV7())
                .onuMac("AA:BB:CC:DD:EE:FF")
                .onuSerial("ZTEG12345678")
                .vlanId(200)
                .downloadSpeed(300)
                .uploadSpeed(150)
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .createdAt(LocalDateTime.now())
                .build();

        when(provisioningService.getProvisioningById(onuId)).thenReturn(Optional.of(onu));

        mockMvc.perform(get("/onus/{id}", onuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(onuId.toString()))
                .andExpect(jsonPath("$.onuSerial").value("ZTEG12345678"))
                .andExpect(jsonPath("$.pppoePassword").doesNotExist());
    }

    @Test
    @DisplayName("GET /onus/contract/{contractId} - Deve retornar ONU vinculada ao contrato")
    void testGetOnuByContractId() throws Exception {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        OnuProvisioning onu = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(UuidCreatorUtils.generateUuidV7())
                .onuMac("AA:BB:CC:11:22:33")
                .onuSerial("FHTT99887766")
                .vlanId(100)
                .downloadSpeed(600)
                .uploadSpeed(300)
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .createdAt(LocalDateTime.now())
                .build();

        when(provisioningService.getProvisioningByContractId(contractId)).thenReturn(Optional.of(onu));

        mockMvc.perform(get("/onus/contract/{contractId}", contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value(contractId.toString()))
                .andExpect(jsonPath("$.pppoePassword").doesNotExist());
    }

    @Test
    @DisplayName("POST /onus/contract/{contractId}/block - Deve bloquear acesso da ONU")
    void testBlockOnu() throws Exception {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        OnuProvisioning blocked = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(UuidCreatorUtils.generateUuidV7())
                .onuMac("AA:BB:CC:11:22:33")
                .onuSerial("FHTT99887766")
                .vlanId(100)
                .downloadSpeed(600)
                .uploadSpeed(300)
                .status(OnuProvisioning.OnuStatus.BLOCKED)
                .createdAt(LocalDateTime.now())
                .build();

        when(provisioningService.blockInternetAccess(eq(contractId), any())).thenReturn(blocked);

        mockMvc.perform(post("/onus/contract/{contractId}/block", contractId)
                        .param("reason", "Atraso de 15 dias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.pppoePassword").doesNotExist());
    }

    @Test
    @DisplayName("POST /onus/contract/{contractId}/unblock - Deve desbloquear acesso da ONU")
    void testUnblockOnu() throws Exception {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        OnuProvisioning unblocked = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(UuidCreatorUtils.generateUuidV7())
                .onuMac("AA:BB:CC:11:22:33")
                .onuSerial("FHTT99887766")
                .vlanId(100)
                .downloadSpeed(600)
                .uploadSpeed(300)
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .createdAt(LocalDateTime.now())
                .build();

        when(provisioningService.unblockInternetAccess(contractId)).thenReturn(unblocked);

        mockMvc.perform(post("/onus/contract/{contractId}/unblock", contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVISIONED"))
                .andExpect(jsonPath("$.pppoePassword").doesNotExist());
    }

    @Test
    @DisplayName("GET /onus/{id}/diagnose - Deve retornar diagnóstico óptico da ONU")
    void testDiagnoseOnu() throws Exception {
        UUID onuId = UuidCreatorUtils.generateUuidV7();
        OnuStatusResponse statusResponse = OnuStatusResponse.builder()
                .onuMac("AA:BB:CC:11:22:33")
                .onuSerial("FHTT99887766")
                .status("ONLINE")
                .rxPowerDbm(new BigDecimal("-18.42"))
                .txPowerDbm(new BigDecimal("2.30"))
                .oltName("OLT Central")
                .ponPort(2)
                .details("Sinal ótimo")
                .build();

        when(provisioningService.diagnoseOnuSignal(onuId)).thenReturn(statusResponse);

        mockMvc.perform(get("/onus/{id}/diagnose", onuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.rxPowerDbm").value(-18.42))
                .andExpect(jsonPath("$.oltName").value("OLT Central"));
    }
}
