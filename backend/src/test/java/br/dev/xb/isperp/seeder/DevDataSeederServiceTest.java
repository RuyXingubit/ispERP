package br.dev.xb.isperp.seeder;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.entity.financial.PayableInvoice;
import br.dev.xb.isperp.entity.financial.UserCashCustody;
import br.dev.xb.isperp.entity.financial.UserMaterialCustody;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.repository.financial.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevDataSeederServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private SiteSettingsRepository siteSettingsRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private InventoryItemRepository inventoryItemRepository;
    @Mock
    private UserMaterialCustodyRepository userMaterialCustodyRepository;
    @Mock
    private UserCashCustodyRepository userCashCustodyRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PayableInvoiceRepository payableInvoiceRepository;
    @Mock
    private ExpenseInstallmentRepository expenseInstallmentRepository;
    @Mock
    private ChartOfAccountRepository chartOfAccountRepository;
    @Mock
    private NetworkProjectRepository networkProjectRepository;
    @Mock
    private FtthCtoRepository ftthCtoRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private DevDataSeederService devDataSeederService;

    @Test
    @DisplayName("Deve ignorar povoamento se já existirem usuários no banco de dados")
    void shouldSkipSeedingIfUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(5L);

        devDataSeederService.run(args);

        verify(companyRepository, never()).save(any());
        verify(customerRepository, never()).save(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve executar povoamento completo de 1 ano quando a base estiver zerada")
    void shouldExecuteFullSeedingWhenDatabaseIsEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPasswordMock");

        // Mock returns para entidades com save
        when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(warehouseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(networkProjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ftthCtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(chartOfAccountRepository.findByCode(anyString())).thenReturn(Optional.empty());

        devDataSeederService.run(args);

        // Verificações
        verify(companyRepository, times(1)).save(any(Company.class));
        verify(siteSettingsRepository, times(1)).save(any(SiteSettings.class));
        verify(userRepository, times(9)).save(any(User.class)); // 1 admin + 1 cfo + 3 atendentes + 4 tecnicos
        verify(planRepository, times(3)).save(any(Plan.class));
        verify(warehouseRepository, times(3)).save(any(Warehouse.class));
        verify(inventoryItemRepository, times(4)).save(any(InventoryItem.class));
        verify(userMaterialCustodyRepository, times(3)).save(any(UserMaterialCustody.class));
        verify(userCashCustodyRepository, times(1)).save(any(UserCashCustody.class));
        verify(networkProjectRepository, times(2)).save(any(NetworkProject.class));
        verify(ftthCtoRepository, times(3)).save(any(FtthCto.class));
        verify(customerRepository, times(13)).save(any(Customer.class));
        verify(contractRepository, times(13)).save(any(Contract.class));
        verify(workOrderRepository, times(1)).save(any(WorkOrder.class));
        verify(invoiceRepository, atLeast(70)).save(any(Invoice.class)); // Histórico de 12 meses
    }
}
