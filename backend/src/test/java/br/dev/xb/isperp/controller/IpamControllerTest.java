package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.ipam.*;
import br.dev.xb.isperp.service.IpamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IpamController.class)
@AutoConfigureMockMvc(addFilters = false)
class IpamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private IpamService ipamService;

    @Test
    @DisplayName("GET /api/ipam/subnets - Deve retornar lista de sub-redes")
    void testGetAllSubnets() throws Exception {
        IpamSubnetResponse response = IpamSubnetResponse.builder()
                .id(UUID.randomUUID())
                .cidr("200.150.10.0/24")
                .ipVersion(IpamIpVersion.IPV4)
                .networkAddress("200.150.10.0")
                .totalHosts(256L)
                .utilizationPercentage(15.5)
                .build();

        when(ipamService.getAllSubnets()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ipam/subnets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cidr").value("200.150.10.0/24"))
                .andExpect(jsonPath("$[0].totalHosts").value(256));
    }

    @Test
    @DisplayName("POST /api/ipam/subnets - Deve criar sub-rede com sucesso")
    void testCreateSubnet() throws Exception {
        IpamSubnetRequest request = IpamSubnetRequest.builder()
                .cidr("10.0.0.0/16")
                .category(IpamSubnetCategory.CGNAT)
                .build();

        IpamSubnetResponse response = IpamSubnetResponse.builder()
                .id(UUID.randomUUID())
                .cidr("10.0.0.0/16")
                .ipVersion(IpamIpVersion.IPV4)
                .totalHosts(65536L)
                .build();

        when(ipamService.createSubnet(any(IpamSubnetRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ipam/subnets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cidr").value("10.0.0.0/16"));
    }

    @Test
    @DisplayName("GET /api/ipam/calculate - Deve retornar cálculo matemático de CIDR")
    void testCalculate() throws Exception {
        SubnetCalculationResult result = SubnetCalculationResult.builder()
                .cidr("192.168.1.0/24")
                .ipVersion(IpamIpVersion.IPV4)
                .networkAddress("192.168.1.0")
                .broadcastAddress("192.168.1.255")
                .usableHosts(254)
                .build();

        when(ipamService.calculate("192.168.1.0/24")).thenReturn(result);

        mockMvc.perform(get("/api/ipam/calculate").param("cidr", "192.168.1.0/24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidr").value("192.168.1.0/24"))
                .andExpect(jsonPath("$.usableHosts").value(254));
    }
}
