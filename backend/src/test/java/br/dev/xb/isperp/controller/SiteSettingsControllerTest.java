package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.SiteSettingsUpdateRequest;
import br.dev.xb.isperp.entity.SiteSettings;
import br.dev.xb.isperp.service.SiteSettingsService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SiteSettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class SiteSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SiteSettingsService siteSettingsService;

    @Test
    @DisplayName("GET /site-settings - Deve retornar configurações com status 200")
    void shouldReturnSiteSettings() throws Exception {
        UUID settingsId = UuidCreatorUtils.generateUuidV7();
        SiteSettings settings = SiteSettings.builder()
                .id(settingsId)
                .siteTitle("Nexus Fibra Telecomunicações")
                .siteDescription("Internet Ultrarrápida")
                .primaryColor("#00bcd4")
                .secondaryColor("#ff4081")
                .createdAt(LocalDateTime.now())
                .build();

        when(siteSettingsService.getSiteSettings()).thenReturn(Optional.of(settings));

        mockMvc.perform(get("/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(settingsId.toString()))
                .andExpect(jsonPath("$.siteTitle").value("Nexus Fibra Telecomunicações"))
                .andExpect(jsonPath("$.primaryColor").value("#00bcd4"));
    }

    @Test
    @DisplayName("PUT /site-settings - Deve atualizar configurações com sucesso")
    void shouldUpdateSiteSettings() throws Exception {
        UUID settingsId = UuidCreatorUtils.generateUuidV7();
        SiteSettings updated = SiteSettings.builder()
                .id(settingsId)
                .siteTitle("Nexus Fibra Atualizada")
                .primaryColor("#112233")
                .createdAt(LocalDateTime.now())
                .build();

        when(siteSettingsService.updateSiteSettings(any())).thenReturn(updated);

        SiteSettingsUpdateRequest request = new SiteSettingsUpdateRequest();
        request.setSiteTitle("Nexus Fibra Atualizada");
        request.setPrimaryColor("#112233");

        mockMvc.perform(put("/site-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(settingsId.toString()))
                .andExpect(jsonPath("$.siteTitle").value("Nexus Fibra Atualizada"));
    }
}
