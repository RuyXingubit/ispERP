package br.dev.xb.isperp.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRequest {

    @NotBlank(message = "Descrição da simulação é obrigatória")
    private String description;

    @NotNull(message = "Valor total do investimento é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser positivo")
    private BigDecimal totalAmount;

    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Mínimo 1 parcela")
    @Builder.Default
    private Integer installmentsCount = 12;

    @NotNull(message = "Data de início da 1ª parcela é obrigatória")
    private LocalDate firstDueDate;
}
