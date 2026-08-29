package br.dev.xb.isperp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.service.CustomerService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança no teste de slice MVC
@SuppressWarnings("null")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("GET /customers - Deve retornar lista de clientes")
    void shouldReturnAllCustomers() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        Customer customer = Customer.builder()
                .id(id)
                .name("Carlos Silva")
                .cpf("52998224725")
                .active(true)
                .build();

        when(customerService.getAllCustomers()).thenReturn(List.of(customer));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /customers/{id} - Deve retornar cliente por UUID")
    void shouldReturnCustomerById() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        Customer customer = Customer.builder()
                .id(id)
                .name("Carlos Silva")
                .cpf("52998224725")
                .active(true)
                .build();

        when(customerService.getCustomerById(id)).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Carlos Silva"));
    }

    @Test
    @DisplayName("POST /customers - Deve criar cliente com sucesso")
    void shouldCreateCustomer() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        Customer input = Customer.builder()
                .name("Novo Cliente")
                .cpf("52998224725")
                .email("novo@cliente.com")
                .build();

        Customer saved = Customer.builder()
                .id(id)
                .name("Novo Cliente")
                .cpf("52998224725")
                .email("novo@cliente.com")
                .active(true)
                .build();

        when(customerService.createCustomer(any(Customer.class))).thenReturn(saved);

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Novo Cliente"));
    }
}
