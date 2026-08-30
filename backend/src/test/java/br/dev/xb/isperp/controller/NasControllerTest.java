package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.NasRequest;
import br.dev.xb.isperp.dto.NasResponse;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.service.NasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NasController.class)
@AutoConfigureMockMvc(addFilters = false)
class NasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NasService nasService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/radius/nas - Deve listar todos os NAS")
    void testGetAllNas() throws Exception {
        NasResponse nas = NasResponse.builder()
                .id(UUID.randomUUID())
                .nasname("10.0.0.1")
                .shortname("BNG-Centro")
                .vendorType(NasVendorType.MIKROTIK)
                .build();

        when(nasService.getAllNas()).thenReturn(List.of(nas));

        mockMvc.perform(get("/api/radius/nas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nasname").value("10.0.0.1"))
                .andExpect(jsonPath("$[0].vendorType").value("MIKROTIK"));
    }

    @Test
    @DisplayName("POST /api/radius/nas - Deve cadastrar novo NAS")
    void testCreateNas() throws Exception {
        NasRequest request = NasRequest.builder()
                .nasname("10.0.0.2")
                .shortname("BNG-Sul")
                .secret("myRadiusSecret")
                .vendorType(NasVendorType.HUAWEI)
                .build();

        NasResponse response = NasResponse.builder()
                .id(UUID.randomUUID())
                .nasname("10.0.0.2")
                .vendorType(NasVendorType.HUAWEI)
                .build();

        when(nasService.createNas(any())).thenReturn(response);

        mockMvc.perform(post("/api/radius/nas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nasname").value("10.0.0.2"))
                .andExpect(jsonPath("$.vendorType").value("HUAWEI"));
    }
}
