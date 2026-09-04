package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.RadiusDisconnectRequest;
import br.dev.xb.isperp.dto.RadiusDisconnectResponse;
import br.dev.xb.isperp.dto.RadiusSessionResponse;
import br.dev.xb.isperp.service.RadiusSessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RadiusSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RadiusSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RadiusSessionService radiusSessionService;

    @Test
    @DisplayName("GET /radius/sessions/active - Deve retornar sessões ativas")
    void testGetActiveSessions() throws Exception {
        RadiusSessionResponse session = RadiusSessionResponse.builder()
                .radacctId(101L)
                .acctSessionId("sess-12345")
                .username("cliente.teste")
                .nasIpAddress("10.0.0.1")
                .nasShortname("bng-01")
                .acctInputOctets(1048576L)
                .acctOutputOctets(2097152L)
                .isOnline(true)
                .customerName("Cliente Teste")
                .build();

        when(radiusSessionService.getActiveSessions()).thenReturn(List.of(session));

        mockMvc.perform(get("/radius/sessions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].radacctId").value(101))
                .andExpect(jsonPath("$[0].username").value("cliente.teste"))
                .andExpect(jsonPath("$[0].isOnline").value(true));
    }

    @Test
    @DisplayName("GET /radius/sessions/history/{username} - Deve retornar histórico de conexões")
    void testGetSessionHistory() throws Exception {
        RadiusSessionResponse session = RadiusSessionResponse.builder()
                .radacctId(202L)
                .acctSessionId("sess-67890")
                .username("cliente.teste")
                .nasIpAddress("10.0.0.1")
                .acctInputOctets(5000L)
                .acctOutputOctets(3000L)
                .isOnline(false)
                .build();

        when(radiusSessionService.getSessionHistoryByUsername("cliente.teste")).thenReturn(List.of(session));

        mockMvc.perform(get("/radius/sessions/history/{username}", "cliente.teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].radacctId").value(202))
                .andExpect(jsonPath("$[0].username").value("cliente.teste"));
    }

    @Test
    @DisplayName("POST /radius/disconnect - Deve enviar PoD e retornar sucesso")
    void testDisconnectUser() throws Exception {
        RadiusDisconnectResponse response = RadiusDisconnectResponse.builder()
                .username("cliente.teste")
                .success(true)
                .message("Usuário desconectado")
                .build();

        when(radiusSessionService.disconnectUser(any(RadiusDisconnectRequest.class))).thenReturn(response);

        String json = """
                {
                    "username": "cliente.teste",
                    "nasIpAddress": "10.0.0.1"
                }
                """;

        mockMvc.perform(post("/radius/disconnect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("cliente.teste"))
                .andExpect(jsonPath("$.success").value(true));
    }
}
