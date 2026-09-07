package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.CashCustodyDto;
import br.dev.xb.isperp.dto.financial.CashTransferResponseDto;
import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import br.dev.xb.isperp.mapper.FinancialDomainMapperImpl;
import br.dev.xb.isperp.service.financial.CashCustodyService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CashCustodyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FinancialDomainMapperImpl.class)
@SuppressWarnings("null")
class CashCustodyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashCustodyService cashCustodyService;

    @Test
    @DisplayName("GET /financial/custody/cash/all - Deve listar todos os saldos sob custódia")
    void shouldGetAllCustodies() throws Exception {
        UUID userId = UuidCreatorUtils.generateUuidV7();
        CashCustodyDto dto = CashCustodyDto.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .userId(userId)
                .userName("Carlos Técnico")
                .userEmail("carlos@isperp.com.br")
                .userRole("TECHNICIAN")
                .cpf("111.222.333-44")
                .currentBalance(new BigDecimal("350.00"))
                .updatedAt(OffsetDateTime.now())
                .build();

        when(cashCustodyService.getAllCustodies()).thenReturn(List.of(dto));

        mockMvc.perform(get("/financial/custody/cash/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("Carlos Técnico"))
                .andExpect(jsonPath("$[0].currentBalance").value(350.00));
    }

    @Test
    @DisplayName("GET /financial/custody/cash/user/{userId} - Deve consultar saldo de colaborador específico")
    void shouldGetCustodyByUserId() throws Exception {
        UUID userId = UuidCreatorUtils.generateUuidV7();
        CashCustodyDto dto = CashCustodyDto.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .userId(userId)
                .userName("Mariana Atendente")
                .userEmail("mariana@isperp.com.br")
                .userRole("ATTENDANT")
                .cpf("222.333.444-55")
                .currentBalance(new BigDecimal("1200.00"))
                .updatedAt(OffsetDateTime.now())
                .build();

        when(cashCustodyService.getCustodyDtoByUserId(userId)).thenReturn(dto);

        mockMvc.perform(get("/financial/custody/cash/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("Mariana Atendente"))
                .andExpect(jsonPath("$.currentBalance").value(1200.00));
    }

    @Test
    @DisplayName("POST /financial/custody/cash/transfer/{id}/respond - Deve responder transferência")
    void shouldRespondTransfer() throws Exception {
        UUID transferId = UuidCreatorUtils.generateUuidV7();
        UUID receiverId = UuidCreatorUtils.generateUuidV7();

        CashTransferResponseDto responseDto = CashTransferResponseDto.builder()
                .id(transferId)
                .amount(new BigDecimal("200.00"))
                .status(CashTransferStatus.ACCEPTED)
                .senderUserName("Carlos Técnico")
                .receiverUserName("Mariana Atendente")
                .build();

        when(cashCustodyService.respondTransfer(receiverId, transferId, true)).thenReturn(responseDto);

        mockMvc.perform(post("/financial/custody/cash/transfer/{id}/respond", transferId)
                        .header("X-User-Id", receiverId.toString())
                        .param("accept", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.amount").value(200.00));
    }
}
