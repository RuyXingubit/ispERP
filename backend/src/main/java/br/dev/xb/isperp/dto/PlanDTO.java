package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {

    private UUID id;

    @NotBlank(message = "Nome do plano é obrigatório")
    private String name;

    @NotNull(message = "Velocidade de download é obrigatória")
    @Positive(message = "Velocidade de download deve ser positiva")
    private Integer downloadSpeed;

    @NotNull(message = "Velocidade de upload é obrigatória")
    @Positive(message = "Velocidade de upload deve ser positiva")
    private Integer uploadSpeed;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    private BigDecimal price;

    private String description;
    private String svaIncluded;
    
    @Builder.Default
    private Boolean active = true;
}
