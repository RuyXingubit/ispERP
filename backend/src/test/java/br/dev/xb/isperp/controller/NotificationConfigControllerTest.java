package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class NotificationConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationConfigRepository configRepository;

    @Test
    @DisplayName("GET /notifications/configs deve listar todas as configurações")
    void testListConfigs() throws Exception {
        NotificationConfig config = NotificationConfig.builder()
                .id(UUID.randomUUID())
                .name("Canal Oficial WhatsApp")
                .providerType(WhatsAppProviderType.EVOLUTION_API)
                .apiUrl("https://api.evolution.nexus.com.br")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(configRepository.findAll()).thenReturn(List.of(config));

        mockMvc.perform(get("/notifications/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Canal Oficial WhatsApp"))
                .andExpect(jsonPath("$[0].providerType").value("EVOLUTION_API"));
    }

    @Test
    @DisplayName("POST /notifications/configs deve salvar nova configuração")
    void testCreateConfig() throws Exception {
        NotificationConfig config = NotificationConfig.builder()
                .id(UUID.randomUUID())
                .name("Twilio Canal")
                .providerType(WhatsAppProviderType.TWILIO)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(configRepository.save(any())).thenReturn(config);

        String json = """
                {
                    "name": "Twilio Canal",
                    "providerType": "TWILIO",
                    "active": true
                }
                """;

        mockMvc.perform(post("/notifications/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Twilio Canal"));
    }
}
