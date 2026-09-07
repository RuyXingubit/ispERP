package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.ChartOfAccountDto;
import br.dev.xb.isperp.entity.financial.AccountType;
import br.dev.xb.isperp.entity.financial.DreCategory;
import br.dev.xb.isperp.mapper.FinancialDomainMapperImpl;
import br.dev.xb.isperp.service.financial.ChartOfAccountService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChartOfAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FinancialDomainMapperImpl.class)
@SuppressWarnings("null")
class ChartOfAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChartOfAccountService chartOfAccountService;

    @Test
    @DisplayName("GET /financial/chart-of-accounts/tree - Deve retornar árvore do plano de contas")
    void shouldGetTree() throws Exception {
        ChartOfAccountDto account = ChartOfAccountDto.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("1.01.01.01")
                .name("Caixa Geral")
                .accountType(AccountType.REVENUE)
                .dreCategory(DreCategory.GROSS_REVENUE)
                .build();

        when(chartOfAccountService.getTree()).thenReturn(List.of(account));

        mockMvc.perform(get("/financial/chart-of-accounts/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("1.01.01.01"))
                .andExpect(jsonPath("$[0].name").value("Caixa Geral"));
    }

    @Test
    @DisplayName("POST /financial/chart-of-accounts - Deve criar nova conta contábil")
    void shouldCreateAccount() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        ChartOfAccountDto created = ChartOfAccountDto.builder()
                .id(id)
                .code("1.01.01.02")
                .name("Banco do Brasil")
                .accountType(AccountType.REVENUE)
                .dreCategory(DreCategory.GROSS_REVENUE)
                .build();

        when(chartOfAccountService.createAccount(any(ChartOfAccountDto.class))).thenReturn(created);

        String json = """
                {
                    "code": "1.01.01.02",
                    "name": "Banco do Brasil",
                    "accountType": "REVENUE",
                    "dreCategory": "GROSS_REVENUE"
                }
                """;

        mockMvc.perform(post("/financial/chart-of-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1.01.01.02"))
                .andExpect(jsonPath("$.name").value("Banco do Brasil"));
    }
}
