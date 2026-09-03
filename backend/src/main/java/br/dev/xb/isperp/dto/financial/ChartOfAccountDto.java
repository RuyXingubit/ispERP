package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.AccountType;
import br.dev.xb.isperp.entity.financial.DreCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccountDto {
    private UUID id;
    private UUID parentId;
    private String parentCode;

    @NotBlank(message = "Código contábil é obrigatório")
    private String code;

    @NotBlank(message = "Nome da conta é obrigatório")
    private String name;

    @NotNull(message = "Tipo de conta é obrigatório")
    private AccountType accountType;

    @NotNull(message = "Categoria DRE é obrigatória")
    private DreCategory dreCategory;

    @Builder.Default
    private Boolean isSynthetic = false;

    @Builder.Default
    private Boolean isAnalytical = true;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private List<ChartOfAccountDto> children = new ArrayList<>();
}
