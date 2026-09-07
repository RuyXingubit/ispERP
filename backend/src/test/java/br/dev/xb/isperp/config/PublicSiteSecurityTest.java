package br.dev.xb.isperp.config;

import br.dev.xb.isperp.controller.CompanyController;
import br.dev.xb.isperp.controller.PlanController;
import br.dev.xb.isperp.controller.SiteSettingsController;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.SiteSettings;
import br.dev.xb.isperp.mapper.CompanyMapper;
import br.dev.xb.isperp.mapper.PlanMapperImpl;
import br.dev.xb.isperp.service.CompanyService;
import br.dev.xb.isperp.service.PlanService;
import br.dev.xb.isperp.service.SiteSettingsService;
import br.dev.xb.isperp.util.JwtUtil;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PlanController.class, SiteSettingsController.class, CompanyController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, PlanMapperImpl.class})
class PublicSiteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PlanService planService;

    @MockitoBean
    private SiteSettingsService siteSettingsService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyMapper companyMapper;

    @Test
    @DisplayName("GET /plans/active - Deve permitir acesso público sem token de autenticação")
    void testGetActivePlansPublicAccess() throws Exception {
        Plan activePlan = Plan.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fibra 500 Mega")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(BigDecimal.valueOf(99.90))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(planService.getActivePlans()).thenReturn(List.of(activePlan));

        mockMvc.perform(get("/plans/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fibra 500 Mega"))
                .andExpect(jsonPath("$[0].downloadSpeed").value(500));
    }

    @Test
    @DisplayName("GET /site-settings - Deve permitir acesso público para carregar tema e cores")
    void testGetSiteSettingsPublicAccess() throws Exception {
        SiteSettings settings = SiteSettings.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .siteTitle("Nexus Fibra")
                .primaryColor("#0284c7")
                .secondaryColor("#10b981")
                .build();

        when(siteSettingsService.getSiteSettings()).thenReturn(Optional.of(settings));

        mockMvc.perform(get("/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siteTitle").value("Nexus Fibra"))
                .andExpect(jsonPath("$.primaryColor").value("#0284c7"));
    }

    @Test
    @DisplayName("GET /companies/primary - Deve permitir acesso público aos dados cadastrais da empresa")
    void testGetPrimaryCompanyPublicAccess() throws Exception {
        when(companyService.getPrimaryCompany()).thenReturn(Optional.empty());

        // Deve retornar 404 (empresa não encontrada) e NUNCA 401/403 (bloqueio de segurança)
        mockMvc.perform(get("/companies/primary"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /plans - Operação de escrita DEVE ser bloqueada (403 Forbidden) para requisição anônima")
    void testPostPlanRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plano Hacker\",\"downloadSpeed\":1000,\"uploadSpeed\":500,\"price\":10.0}"))
                .andExpect(status().isForbidden());
    }
}
