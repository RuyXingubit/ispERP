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
public class NetworkProjectRequest {

    @NotBlank(message = "Nome do projeto é obrigatório")
    private String name;

    @NotBlank(message = "Bairro é obrigatório")
    private String neighborhood;

    @NotBlank(message = "Cidade é obrigatória")
    private String city;

    @NotNull(message = "Orçamento de investimento (CAPEX) é obrigatório")
    @DecimalMin(value = "0.01", message = "Orçamento deve ser maior que zero")
    private BigDecimal budgetAmount;

    @NotNull(message = "Meta de assinantes é obrigatória")
    @Min(value = 1, message = "Meta deve ser de ao menos 1 assinante")
    @Builder.Default
    private Integer targetSubscribers = 100;

    @Builder.Default
    private LocalDate startDate = LocalDate.now();

    private String notes;
}
