package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.MaterialCustodyDto;
import br.dev.xb.isperp.dto.financial.MaterialTransferRequest;
import br.dev.xb.isperp.dto.financial.MaterialTransferResponseDto;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import br.dev.xb.isperp.entity.financial.MaterialTransferLog;
import br.dev.xb.isperp.entity.financial.MaterialType;
import br.dev.xb.isperp.entity.financial.UserMaterialCustody;
import br.dev.xb.isperp.mapper.CustodyMapper;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.financial.MaterialTransferLogRepository;
import br.dev.xb.isperp.repository.financial.UserMaterialCustodyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialCustodyServiceTest {

    @Mock
    private UserMaterialCustodyRepository materialCustodyRepository;

    @Mock
    private MaterialTransferLogRepository materialTransferRepository;

    @Mock
    private UserRepository userRepository;

    private final CustodyMapper custodyMapper = Mappers.getMapper(CustodyMapper.class);

    private MaterialCustodyService materialCustodyService;

    private User techCarlos;
    private User techMarcos;
    private UUID techCarlosId;
    private UUID techMarcosId;

    @BeforeEach
    void setUp() {
        materialCustodyService = new MaterialCustodyService(
                materialCustodyRepository,
                materialTransferRepository,
                userRepository,
                custodyMapper
        );

        techCarlosId = UUID.randomUUID();
        techCarlos = User.builder()
                .id(techCarlosId)
                .name("Carlos Técnico")
                .email("carlos@isperp.com")
                .cpf("111.222.333-44")
                .role(UserRole.TECHNICIAN)
                .build();

        techMarcosId = UUID.randomUUID();
        techMarcos = User.builder()
                .id(techMarcosId)
                .name("Marcos Técnico")
                .email("marcos@isperp.com")
                .cpf("222.333.444-55")
                .role(UserRole.TECHNICIAN)
                .build();
    }

    @Test
    @DisplayName("Deve alocar carga patrimonial de ONT no CPF do técnico Carlos")
    void shouldAllocateMaterialToTechnicianCpf() {
        when(userRepository.findById(techCarlosId)).thenReturn(Optional.of(techCarlos));
        when(materialCustodyRepository.save(any(UserMaterialCustody.class))).thenAnswer(i -> i.getArgument(0));

        MaterialCustodyDto dto = MaterialCustodyDto.builder()
                .itemName("ONT Huawei Wi-Fi 6 GPON")
                .itemType(MaterialType.ONT)
                .serialNumber("HWTC12345678")
                .macAddress("00:11:22:33:44:55")
                .quantity(BigDecimal.ONE)
                .build();

        MaterialCustodyDto result = materialCustodyService.allocateMaterialToUser(techCarlosId, dto);

        assertThat(result.getItemName()).isEqualTo("ONT Huawei Wi-Fi 6 GPON");
        assertThat(result.getSerialNumber()).isEqualTo("HWTC12345678");
        assertThat(result.getUserName()).isEqualTo("Carlos Técnico");
    }

    @Test
    @DisplayName("Deve transferir carga de equipamento entre técnicos com duplo aceite")
    void shouldTransferMaterialWithDualAcceptance() {
        UUID custodyId = UUID.randomUUID();
        UUID transferLogId = UUID.randomUUID();

        UserMaterialCustody custody = UserMaterialCustody.builder()
                .id(custodyId)
                .user(techCarlos)
                .itemName("Máquina de Fusão Óptica")
                .itemType(MaterialType.FUSION_MACHINE)
                .serialNumber("FUSION-9988")
                .quantity(BigDecimal.ONE)
                .build();

        MaterialTransferLog transferLog = MaterialTransferLog.builder()
                .id(transferLogId)
                .sender(techCarlos)
                .receiver(techMarcos)
                .materialCustody(custody)
                .quantity(BigDecimal.ONE)
                .status(CashTransferStatus.PENDING_ACCEPTANCE)
                .build();

        when(materialTransferRepository.findById(transferLogId)).thenReturn(Optional.of(transferLog));

        MaterialTransferResponseDto result = materialCustodyService.respondMaterialTransfer(techMarcosId, transferLogId, true);

        assertThat(result.getStatus()).isEqualTo(CashTransferStatus.ACCEPTED);
        assertThat(custody.getUser().getId()).isEqualTo(techMarcosId);
    }

    @Test
    @DisplayName("Deve consumir automaticamente o equipamento da carga do técnico ao concluir O.S.")
    void shouldConsumeMaterialOnWorkOrderCompletion() {
        UserMaterialCustody custody = UserMaterialCustody.builder()
                .id(UUID.randomUUID())
                .user(techCarlos)
                .itemName("ONT GPON")
                .serialNumber("ONT-SN-4455")
                .quantity(BigDecimal.ONE)
                .build();

        when(materialCustodyRepository.findBySerialNumber("ONT-SN-4455")).thenReturn(Optional.of(custody));

        materialCustodyService.consumeMaterialOnWorkOrder(techCarlosId, "ONT-SN-4455", BigDecimal.ONE);

        verify(materialCustodyRepository).delete(custody);
    }
}
