package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.signature.FallbackMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FallbackSelectionRequest {

    @NotNull(message = "O método de fallback é obrigatório")
    private FallbackMethod fallbackMethod;

    private String justification;
}
