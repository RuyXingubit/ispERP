package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.service.InitialSetupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InitialSetupController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class InitialSetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitialSetupService initialSetupService;

    @Test
    @DisplayName("GET /initial-setup/status deve retornar status de conclusão do setup")
    void testGetSetupStatus() throws Exception {
        when(initialSetupService.isSetupCompleted()).thenReturn(true);

        mockMvc.perform(get("/initial-setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSetupCompleted").value(true));
    }

    @Test
    @DisplayName("POST /initial-setup deve realizar setup quando não concluído")
    void testPerformInitialSetupSuccess() throws Exception {
        when(initialSetupService.isSetupCompleted()).thenReturn(false);
        doNothing().when(initialSetupService).performSetup(any());

        String json = """
                {
                    "adminName": "Administrador ispERP",
                    "adminEmail": "admin@nexusfibra.com.br",
                    "adminPassword": "Password@2026",
                    "companyName": "Nexus Fibra Ltda",
                    "siteTitle": "Nexus Fibra"
                }
                """;

        mockMvc.perform(post("/initial-setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isSetupCompleted").value(true));
    }
}
