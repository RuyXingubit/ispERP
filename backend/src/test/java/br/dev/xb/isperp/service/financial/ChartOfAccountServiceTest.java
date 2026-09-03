package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.ChartOfAccountDto;
import br.dev.xb.isperp.entity.financial.AccountType;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.DreCategory;
import br.dev.xb.isperp.mapper.FinancialAccountMapper;
import br.dev.xb.isperp.repository.financial.ChartOfAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChartOfAccountServiceTest {

    @Mock
    private ChartOfAccountRepository chartOfAccountRepository;

    private final FinancialAccountMapper mapper = Mappers.getMapper(FinancialAccountMapper.class);

    private ChartOfAccountService chartOfAccountService;

    @BeforeEach
    void setUp() {
        chartOfAccountService = new ChartOfAccountService(chartOfAccountRepository, mapper);
    }

    @Test
    @DisplayName("Deve impedir criação de conta contábil com código duplicado")
    void shouldRejectDuplicateCode() {
        ChartOfAccount existing = ChartOfAccount.builder()
                .id(UUID.randomUUID())
                .code("01.01.01")
                .name("Mensalidades Fibra")
                .build();

        when(chartOfAccountRepository.findByCode("01.01.01")).thenReturn(Optional.of(existing));

        ChartOfAccountDto dto = ChartOfAccountDto.builder()
                .code("01.01.01")
                .name("Outra Conta")
                .accountType(AccountType.REVENUE)
                .dreCategory(DreCategory.GROSS_REVENUE)
                .build();

        assertThatThrownBy(() -> chartOfAccountService.createAccount(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Já existe uma conta contábil cadastrada com o código");
    }

    @Test
    @DisplayName("Deve montar a árvore hierárquica a partir dos nós raízes")
    void shouldBuildHierarchicalTree() {
        UUID rootId = UUID.randomUUID();
        ChartOfAccount root = ChartOfAccount.builder()
                .id(rootId)
                .code("01")
                .name("01. RECEITAS")
                .accountType(AccountType.REVENUE)
                .dreCategory(DreCategory.GROSS_REVENUE)
                .isSynthetic(true)
                .build();

        ChartOfAccount child = ChartOfAccount.builder()
                .id(UUID.randomUUID())
                .parent(root)
                .code("01.01")
                .name("01.01. Mensalidades")
                .accountType(AccountType.REVENUE)
                .dreCategory(DreCategory.GROSS_REVENUE)
                .isAnalytical(true)
                .build();

        when(chartOfAccountRepository.findByParentIsNullOrderByCodeAsc()).thenReturn(List.of(root));
        when(chartOfAccountRepository.findByParentIdOrderByCodeAsc(rootId)).thenReturn(List.of(child));

        List<ChartOfAccountDto> tree = chartOfAccountService.getTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getCode()).isEqualTo("01");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getCode()).isEqualTo("01.01");
    }
}
