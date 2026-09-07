package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.MaterialCustodyDto;
import br.dev.xb.isperp.dto.financial.MaterialTransferResponseDto;
import br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto;
import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import br.dev.xb.isperp.entity.financial.MaterialType;
import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.entity.financial.ProjectStatus;
import br.dev.xb.isperp.mapper.FinancialDomainMapperImpl;
import br.dev.xb.isperp.service.financial.MaterialCustodyService;
import br.dev.xb.isperp.service.financial.NetworkProjectService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {MaterialCustodyController.class, NetworkProjectController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(FinancialDomainMapperImpl.class)
@SuppressWarnings("null")
class MaterialCustodyAndNetworkProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialCustodyService materialCustodyService;

    @MockitoBean
    private NetworkProjectService networkProjectService;

    @Nested
    @DisplayName("MaterialCustodyController Tests")
    class MaterialCustodyTests {

        @Test
        @DisplayName("GET /financial/custody/materials/user/{userId} - Deve listar materiais de um usuário")
        void shouldGetMaterialsByUserId() throws Exception {
            UUID userId = UuidCreatorUtils.generateUuidV7();
            MaterialCustodyDto dto = MaterialCustodyDto.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .userId(userId)
                    .userName("Técnico 1")
                    .itemName("ONU Huawei")
                    .itemType(MaterialType.ONT)
                    .serialNumber("HW123456")
                    .quantity(BigDecimal.ONE)
                    .unit("UN")
                    .allocatedAt(OffsetDateTime.now())
                    .build();

            when(materialCustodyService.getMaterialsByUserId(userId)).thenReturn(List.of(dto));

            mockMvc.perform(get("/financial/custody/materials/user/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].itemName").value("ONU Huawei"))
                    .andExpect(jsonPath("$[0].serialNumber").value("HW123456"));
        }

        @Test
        @DisplayName("POST /financial/custody/materials/user/{userId}/allocate - Deve alocar material")
        void shouldAllocateMaterial() throws Exception {
            UUID userId = UuidCreatorUtils.generateUuidV7();
            MaterialCustodyDto allocated = MaterialCustodyDto.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .userId(userId)
                    .itemName("Conector SC/APC")
                    .quantity(new BigDecimal("100"))
                    .unit("UN")
                    .build();

            when(materialCustodyService.allocateMaterialToUser(eq(userId), any())).thenReturn(allocated);

            String requestBody = """
                    {
                        "itemName": "Conector SC/APC",
                        "quantity": 100,
                        "unit": "UN"
                    }
                    """;

            mockMvc.perform(post("/financial/custody/materials/user/{userId}/allocate", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.itemName").value("Conector SC/APC"));
        }

        @Test
        @DisplayName("POST /financial/custody/materials/transfer/request - Deve solicitar transferência")
        void shouldRequestMaterialTransfer() throws Exception {
            UUID fromUserId = UuidCreatorUtils.generateUuidV7();
            MaterialTransferResponseDto responseDto = MaterialTransferResponseDto.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .senderUserId(fromUserId)
                    .itemName("Cabo Drop")
                    .status(CashTransferStatus.PENDING_ACCEPTANCE)
                    .build();

            when(materialCustodyService.requestMaterialTransfer(eq(fromUserId), any())).thenReturn(responseDto);

            String requestBody = """
                    {
                        "receiverUserId": "018d9f4e-2b5e-7a4c-9f8e-123456789abc",
                        "materialCustodyId": "018d9f4e-2b5e-7a4c-9f8e-987654321def",
                        "quantity": 200
                    }
                    """;

            mockMvc.perform(post("/financial/custody/materials/transfer/request")
                            .header("X-User-Id", fromUserId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.itemName").value("Cabo Drop"))
                    .andExpect(jsonPath("$.status").value("PENDING_ACCEPTANCE"));
        }
    }

    @Nested
    @DisplayName("NetworkProjectController Tests")
    class NetworkProjectTests {

        @Test
        @DisplayName("GET /financial/network-projects/payback - Deve retornar lista de payback dos projetos")
        void shouldGetAllProjectsWithPayback() throws Exception {
            NetworkProjectPaybackDto paybackDto = NetworkProjectPaybackDto.builder()
                    .projectId(UuidCreatorUtils.generateUuidV7())
                    .name("Expansão Bairro Alto")
                    .neighborhood("Alto da Colina")
                    .budgetAmount(new BigDecimal("50000.00"))
                    .status(ProjectStatus.ACTIVE)
                    .build();

            when(networkProjectService.getAllProjectsWithPayback()).thenReturn(List.of(paybackDto));

            mockMvc.perform(get("/financial/network-projects/payback")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Expansão Bairro Alto"))
                    .andExpect(jsonPath("$[0].budgetAmount").value(50000.00));
        }

        @Test
        @DisplayName("POST /financial/network-projects - Deve criar novo projeto de rede")
        void shouldCreateNetworkProject() throws Exception {
            UUID projectId = UuidCreatorUtils.generateUuidV7();
            NetworkProject project = NetworkProject.builder()
                    .id(projectId)
                    .name("FTTH Zona Sul")
                    .neighborhood("Sul")
                    .city("Curitiba")
                    .budgetAmount(new BigDecimal("120000.00"))
                    .targetSubscribers(200)
                    .startDate(LocalDate.of(2026, 3, 1))
                    .status(ProjectStatus.ACTIVE)
                    .build();

            when(networkProjectService.createProject(any())).thenReturn(project);

            String requestBody = """
                    {
                        "name": "FTTH Zona Sul",
                        "neighborhood": "Sul",
                        "city": "Curitiba",
                        "budgetAmount": 120000.00,
                        "targetSubscribers": 200,
                        "startDate": "2026-03-01"
                    }
                    """;

            mockMvc.perform(post("/financial/network-projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(projectId.toString()))
                    .andExpect(jsonPath("$.name").value("FTTH Zona Sul"));
        }

        @Test
        @DisplayName("POST /financial/network-projects/{projectId}/assign-cto/{ctoId} - Deve associar CTO ao projeto")
        void shouldAssignCtoToProject() throws Exception {
            UUID projectId = UuidCreatorUtils.generateUuidV7();
            UUID ctoId = UuidCreatorUtils.generateUuidV7();

            mockMvc.perform(post("/financial/network-projects/{projectId}/assign-cto/{ctoId}", projectId, ctoId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(networkProjectService).assignCtoToProject(ctoId, projectId);
        }
    }
}
