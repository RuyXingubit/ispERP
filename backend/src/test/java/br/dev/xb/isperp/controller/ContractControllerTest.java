package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.ContractCreateRequest;
import br.dev.xb.isperp.api.dto.ContractResponse;
import br.dev.xb.isperp.api.dto.ContractStatus;
import br.dev.xb.isperp.api.dto.ContractUpdateRequest;
import br.dev.xb.isperp.api.dto.UpdateContractStatusRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.mapper.ContractMapper;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContractController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private ContractMapper contractMapper;

    private UUID contractId;
    private UUID customerId;
    private UUID planId;
    private Contract contract;
    private ContractResponse contractResponse;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();

        contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-2026-00101")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("129.90"))
                .dueDay(15)
                .installationAddress("Rua das Palmeiras, 450")
                .city("Santarém")
                .state("PA")
                .zipCode("68000-000")
                .build();

        contractResponse = new ContractResponse();
        contractResponse.setId(contractId);
        contractResponse.setCustomerId(customerId);
        contractResponse.setPlanId(planId);
        contractResponse.setContractNumber("CTR-2026-00101");
        contractResponse.setStatus(ContractStatus.ACTIVE);
        contractResponse.setMonthlyFee(129.90);
        contractResponse.setDueDay(15);
        contractResponse.setInstallationAddress("Rua das Palmeiras, 450");
        contractResponse.setCity("Santarém");
        contractResponse.setState("PA");
        contractResponse.setZipCode("68000-000");
    }

    @Test
    @DisplayName("GET /contracts - Deve retornar lista de contratos")
    void shouldGetAllContracts() throws Exception {
        when(contractService.getAllContracts()).thenReturn(List.of(contract));
        when(contractMapper.toResponseList(any())).thenReturn(List.of(contractResponse));

        mockMvc.perform(get("/contracts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(contractId.toString()))
                .andExpect(jsonPath("$[0].contractNumber").value("CTR-2026-00101"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(contractService).getAllContracts();
    }

    @Test
    @DisplayName("GET /contracts/{id} - Deve retornar contrato existente")
    void shouldGetContractByIdWhenFound() throws Exception {
        when(contractService.getContractById(contractId)).thenReturn(Optional.of(contract));
        when(contractMapper.toResponse(contract)).thenReturn(contractResponse);

        mockMvc.perform(get("/contracts/{id}", contractId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId.toString()))
                .andExpect(jsonPath("$.contractNumber").value("CTR-2026-00101"))
                .andExpect(jsonPath("$.city").value("Santarém"));

        verify(contractService).getContractById(contractId);
    }

    @Test
    @DisplayName("GET /contracts/{id} - Deve retornar 404 quando contrato não existe")
    void shouldReturnNotFoundWhenContractByIdNotFound() throws Exception {
        when(contractService.getContractById(contractId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/contracts/{id}", contractId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(contractService).getContractById(contractId);
    }

    @Test
    @DisplayName("GET /contracts/customer/{customerId} - Deve retornar contratos de um cliente")
    void shouldGetContractsByCustomerId() throws Exception {
        when(contractService.getContractsByCustomerId(customerId)).thenReturn(List.of(contract));
        when(contractMapper.toResponseList(any())).thenReturn(List.of(contractResponse));

        mockMvc.perform(get("/contracts/customer/{customerId}", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));

        verify(contractService).getContractsByCustomerId(customerId);
    }

    @Test
    @DisplayName("GET /contracts/status/{status} - Deve filtrar contratos por status")
    void shouldGetContractsByStatus() throws Exception {
        when(contractMapper.toEntityStatus(ContractStatus.ACTIVE)).thenReturn(Contract.ContractStatus.ACTIVE);
        when(contractService.getContractsByStatus(Contract.ContractStatus.ACTIVE)).thenReturn(List.of(contract));
        when(contractMapper.toResponseList(any())).thenReturn(List.of(contractResponse));

        mockMvc.perform(get("/contracts/status/{status}", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        verify(contractService).getContractsByStatus(Contract.ContractStatus.ACTIVE);
    }

    @Test
    @DisplayName("POST /contracts - Deve criar contrato com sucesso")
    void shouldCreateContract() throws Exception {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setCustomerId(customerId);
        request.setPlanId(planId);
        request.setContractNumber("CTR-2026-00102");
        request.setStatus(ContractStatus.PENDING_INSTALLATION);
        request.setMonthlyFee(119.90);
        request.setDueDay(10);
        request.setInstallationAddress("Av. Brasil, 100");

        when(contractMapper.toEntity(any(ContractCreateRequest.class))).thenReturn(contract);
        when(contractService.createContract(any(Contract.class))).thenReturn(contract);
        when(contractMapper.toResponse(contract)).thenReturn(contractResponse);

        mockMvc.perform(post("/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(contractId.toString()));

        verify(contractService).createContract(any(Contract.class));
    }

    @Test
    @DisplayName("PUT /contracts/{id} - Deve atualizar contrato existente")
    void shouldUpdateContract() throws Exception {
        ContractUpdateRequest request = new ContractUpdateRequest();
        request.setMonthlyFee(139.90);
        request.setDueDay(20);

        when(contractService.getContractById(contractId)).thenReturn(Optional.of(contract));
        doNothing().when(contractMapper).updateEntityFromRequest(any(ContractUpdateRequest.class), eq(contract));
        when(contractService.updateContract(contract)).thenReturn(contract);
        when(contractMapper.toResponse(contract)).thenReturn(contractResponse);

        mockMvc.perform(put("/contracts/{id}", contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId.toString()));

        verify(contractService).updateContract(contract);
    }

    @Test
    @DisplayName("PUT /contracts/{id} - Deve retornar 404 quando contrato não existe")
    void shouldReturnNotFoundWhenUpdatingNonExistentContract() throws Exception {
        ContractUpdateRequest request = new ContractUpdateRequest();
        when(contractService.getContractById(contractId)).thenReturn(Optional.empty());

        mockMvc.perform(put("/contracts/{id}", contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /contracts/{id}/status - Deve atualizar status via query parameter")
    void shouldUpdateContractStatusViaQueryParam() throws Exception {
        when(contractMapper.toEntityStatus(ContractStatus.SUSPENDED)).thenReturn(Contract.ContractStatus.SUSPENDED);
        when(contractService.updateStatus(contractId, Contract.ContractStatus.SUSPENDED)).thenReturn(contract);

        contractResponse.setStatus(ContractStatus.SUSPENDED);
        when(contractMapper.toResponse(contract)).thenReturn(contractResponse);

        mockMvc.perform(patch("/contracts/{id}/status", contractId)
                        .param("status", "SUSPENDED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(contractService).updateStatus(contractId, Contract.ContractStatus.SUSPENDED);
    }

    @Test
    @DisplayName("PATCH /contracts/{id}/status - Deve atualizar status via corpo da requisição")
    void shouldUpdateContractStatusViaRequestBody() throws Exception {
        UpdateContractStatusRequest body = new UpdateContractStatusRequest();
        body.setStatus(ContractStatus.CANCELLED);
        body.setReason("Inadimplência reiterada");

        when(contractMapper.toEntityStatus(ContractStatus.CANCELLED)).thenReturn(Contract.ContractStatus.CANCELED);
        when(contractService.updateStatus(contractId, Contract.ContractStatus.CANCELED)).thenReturn(contract);

        contractResponse.setStatus(ContractStatus.CANCELLED);
        when(contractMapper.toResponse(contract)).thenReturn(contractResponse);

        mockMvc.perform(patch("/contracts/{id}/status", contractId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(contractService).updateStatus(contractId, Contract.ContractStatus.CANCELED);
    }

    @Test
    @DisplayName("PATCH /contracts/{id}/status - Deve retornar 400 se nenhum status for fornecido")
    void shouldReturnBadRequestWhenNoStatusProvided() throws Exception {
        mockMvc.perform(patch("/contracts/{id}/status", contractId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /contracts/{id}/status - Deve retornar 404 quando contrato não existe")
    void shouldReturnNotFoundWhenContractStatusUpdateFails() throws Exception {
        when(contractMapper.toEntityStatus(ContractStatus.ACTIVE)).thenReturn(Contract.ContractStatus.ACTIVE);
        when(contractService.updateStatus(contractId, Contract.ContractStatus.ACTIVE))
                .thenThrow(new RuntimeException("Contrato não encontrado"));

        mockMvc.perform(patch("/contracts/{id}/status", contractId)
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
