package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.CustomerCreateRequest;
import br.dev.xb.isperp.api.dto.CustomerResponse;
import br.dev.xb.isperp.api.dto.CustomerUpdateRequest;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.mapper.CustomerMapper;
import br.dev.xb.isperp.service.CustomerService;
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

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private CustomerMapper customerMapper;

    private UUID customerId;
    private Customer customer;
    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();

        customer = Customer.builder()
                .id(customerId)
                .name("Maria Silva Santos")
                .cpf("12345678901")
                .email("maria.silva@nexusfibra.com.br")
                .phone("(91) 98765-4321")
                .active(true)
                .portalPin("1234")
                .build();

        customerResponse = new CustomerResponse();
        customerResponse.setId(customerId);
        customerResponse.setName("Maria Silva Santos");
        customerResponse.setCpf("12345678901");
        customerResponse.setEmail("maria.silva@nexusfibra.com.br");
        customerResponse.setPhone("(91) 98765-4321");
        customerResponse.setActive(true);
    }

    @Test
    @DisplayName("GET /customers - Deve retornar todos os clientes")
    void shouldGetAllCustomers() throws Exception {
        when(customerService.getAllCustomers()).thenReturn(List.of(customer));
        when(customerMapper.toResponseList(any())).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(customerId.toString()))
                .andExpect(jsonPath("$[0].name").value("Maria Silva Santos"))
                .andExpect(jsonPath("$[0].portalPin").doesNotExist()); // Valida que portalPin NÃO é exposto
    }

    @Test
    @DisplayName("GET /customers/active - Deve retornar apenas clientes ativos")
    void shouldGetActiveCustomers() throws Exception {
        when(customerService.getActiveCustomers()).thenReturn(List.of(customer));
        when(customerMapper.toResponseList(any())).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/customers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("GET /customers/{id} - Deve retornar cliente por ID")
    void shouldGetCustomerById() throws Exception {
        when(customerService.getCustomerById(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        mockMvc.perform(get("/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.name").value("Maria Silva Santos"));
    }

    @Test
    @DisplayName("GET /customers/{id} - Deve retornar 404 quando cliente não existir")
    void shouldReturn404WhenNotFound() throws Exception {
        when(customerService.getCustomerById(customerId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/customers/{id}", customerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /customers/cpf/{cpf} - Deve retornar cliente por CPF")
    void shouldGetCustomerByCpf() throws Exception {
        when(customerService.getCustomerByCpf("12345678901")).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        mockMvc.perform(get("/customers/cpf/{cpf}", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("12345678901"));
    }

    @Test
    @DisplayName("GET /customers/search/name - Deve buscar clientes por nome")
    void shouldSearchByName() throws Exception {
        when(customerService.searchCustomersByName("Maria")).thenReturn(List.of(customer));
        when(customerMapper.toResponseList(any())).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/customers/search/name").param("name", "Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maria Silva Santos"));
    }

    @Test
    @DisplayName("GET /customers/search/cpf - Deve buscar clientes por CPF")
    void shouldSearchByCpf() throws Exception {
        when(customerService.searchCustomersByCpf("123")).thenReturn(List.of(customer));
        when(customerMapper.toResponseList(any())).thenReturn(List.of(customerResponse));

        mockMvc.perform(get("/customers/search/cpf").param("cpf", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpf").value("12345678901"));
    }

    @Test
    @DisplayName("POST /customers - Deve cadastrar cliente")
    void shouldCreateCustomer() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setName("Novo Assinante");
        request.setCpf("98765432100");

        when(customerMapper.toEntity(any())).thenReturn(customer);
        when(customerService.createCustomer(any())).thenReturn(customer);
        when(customerMapper.toResponse(any())).thenReturn(customerResponse);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()));
    }

    @Test
    @DisplayName("PUT /customers/{id} - Deve atualizar cliente")
    void shouldUpdateCustomer() throws Exception {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setName("Nome Atualizado");

        when(customerService.updateCustomer(eq(customerId), any())).thenReturn(customer);
        when(customerMapper.toResponse(any())).thenReturn(customerResponse);

        mockMvc.perform(put("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /customers/{id} - Deve retornar 204 No Content")
    void shouldDeleteCustomer() throws Exception {
        doNothing().when(customerService).deleteCustomer(customerId);

        mockMvc.perform(delete("/customers/{id}", customerId))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(customerId);
    }

    @Test
    @DisplayName("PATCH /customers/{id}/activate - Deve ativar cliente")
    void shouldActivateCustomer() throws Exception {
        doNothing().when(customerService).activateCustomer(customerId);

        mockMvc.perform(patch("/customers/{id}/activate", customerId))
                .andExpect(status().isOk());

        verify(customerService).activateCustomer(customerId);
    }

    @Test
    @DisplayName("PATCH /customers/{id}/deactivate - Deve desativar cliente")
    void shouldDeactivateCustomer() throws Exception {
        doNothing().when(customerService).deactivateCustomer(customerId);

        mockMvc.perform(patch("/customers/{id}/deactivate", customerId))
                .andExpect(status().isOk());

        verify(customerService).deactivateCustomer(customerId);
    }
}
