package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer validCustomer;
    private final String VALID_CPF = "52998224725";

    @BeforeEach
    void setUp() {
        validCustomer = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Maria Oliveira")
                .cpf("529.982.247-25")
                .email("maria@provedor.com.br")
                .phone("11987654321")
                .address("Rua das Flores, 123")
                .city("São Paulo")
                .state("SP")
                .zipCode("01000-000")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve criar cliente com sucesso e CPF limpo")
    void shouldCreateCustomerSuccessfully() {
        when(customerRepository.existsByCpf(VALID_CPF)).thenReturn(false);
        when(customerRepository.existsByEmail("maria@provedor.com.br")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = customerService.createCustomer(validCustomer);

        assertNotNull(created);
        assertEquals(VALID_CPF, created.getCpf(), "CPF deve ser salvo sem pontos ou traços");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve rejeitar criação de cliente com CPF inválido")
    void shouldThrowExceptionWhenCpfIsInvalid() {
        validCustomer.setCpf("11111111111");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> customerService.createCustomer(validCustomer));
        assertEquals("CPF inválido", exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar criação de cliente com CPF duplicado")
    void shouldThrowExceptionWhenCpfAlreadyExists() {
        when(customerRepository.existsByCpf(VALID_CPF)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> customerService.createCustomer(validCustomer));
        assertEquals("CPF já cadastrado", exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar cliente por UUID")
    void shouldFindCustomerById() {
        UUID customerId = validCustomer.getId();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));

        Optional<Customer> found = customerService.getCustomerById(customerId);

        assertTrue(found.isPresent());
        assertEquals("Maria Oliveira", found.get().getName());
        assertEquals(customerId, found.get().getId());
    }

    @Test
    @DisplayName("Deve desativar e ativar cliente por UUID")
    void shouldDeactivateAndActivateCustomer() {
        UUID customerId = validCustomer.getId();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(validCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(validCustomer);

        customerService.deactivateCustomer(customerId);
        assertFalse(validCustomer.getActive());

        customerService.activateCustomer(customerId);
        assertTrue(validCustomer.getActive());

        verify(customerRepository, times(2)).save(validCustomer);
    }
}
