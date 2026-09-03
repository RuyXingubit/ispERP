package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetClientPinRequest {

    @NotNull(message = "ID do cliente é obrigatório")
    private UUID customerId;

    private String currentPin;

    @NotBlank(message = "Novo PIN é obrigatório")
    @Pattern(regexp = "^\\d{4}$", message = "O PIN deve conter exatamente 4 dígitos numéricos")
    private String newPin;
}
