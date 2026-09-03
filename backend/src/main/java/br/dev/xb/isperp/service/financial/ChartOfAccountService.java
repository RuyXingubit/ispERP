package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.ChartOfAccountDto;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.mapper.FinancialAccountMapper;
import br.dev.xb.isperp.repository.financial.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountService {

    private final ChartOfAccountRepository chartOfAccountRepository;
    private final FinancialAccountMapper financialAccountMapper;

    @Transactional(readOnly = true)
    public List<ChartOfAccountDto> getTree() {
        List<ChartOfAccount> roots = chartOfAccountRepository.findByParentIsNullOrderByCodeAsc();
        List<ChartOfAccountDto> tree = new ArrayList<>();
        for (ChartOfAccount root : roots) {
            tree.add(buildTreeDto(root));
        }
        return tree;
    }

    private ChartOfAccountDto buildTreeDto(ChartOfAccount node) {
        ChartOfAccountDto dto = financialAccountMapper.toDto(node);
        List<ChartOfAccount> children = chartOfAccountRepository.findByParentIdOrderByCodeAsc(node.getId());
        List<ChartOfAccountDto> childrenDto = new ArrayList<>();
        for (ChartOfAccount child : children) {
            childrenDto.add(buildTreeDto(child));
        }
        dto.setChildren(childrenDto);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ChartOfAccountDto> getAllFlat() {
        return financialAccountMapper.toChartOfAccountDtoList(chartOfAccountRepository.findAll());
    }

    @Transactional
    public ChartOfAccountDto createAccount(ChartOfAccountDto dto) {
        if (chartOfAccountRepository.findByCode(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma conta contábil cadastrada com o código: " + dto.getCode());
        }

        ChartOfAccount parent = null;
        if (dto.getParentId() != null) {
            parent = chartOfAccountRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta contábil pai não encontrada: " + dto.getParentId()));
            if (!Boolean.TRUE.equals(parent.getIsSynthetic())) {
                parent.setIsSynthetic(true);
                parent.setIsAnalytical(false);
                chartOfAccountRepository.save(parent);
            }
        }

        ChartOfAccount account = ChartOfAccount.builder()
                .parent(parent)
                .code(dto.getCode())
                .name(dto.getName())
                .accountType(dto.getAccountType())
                .dreCategory(dto.getDreCategory())
                .isSynthetic(dto.getIsSynthetic() != null ? dto.getIsSynthetic() : false)
                .isAnalytical(dto.getIsAnalytical() != null ? dto.getIsAnalytical() : true)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        ChartOfAccount saved = chartOfAccountRepository.save(account);
        log.info("Conta contábil criada: {} - {}", saved.getCode(), saved.getName());
        return financialAccountMapper.toDto(saved);
    }

    @Transactional
    public ChartOfAccountDto updateAccount(UUID id, ChartOfAccountDto dto) {
        ChartOfAccount account = chartOfAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta contábil não encontrada: " + id));

        account.setName(dto.getName());
        account.setAccountType(dto.getAccountType());
        account.setDreCategory(dto.getDreCategory());
        if (dto.getActive() != null) {
            account.setActive(dto.getActive());
        }

        ChartOfAccount updated = chartOfAccountRepository.save(account);
        return financialAccountMapper.toDto(updated);
    }
}
