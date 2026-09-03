package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAuthRequest {

    @NotBlank(message = "CPF ou CNPJ é obrigatório")
    private String document;

    private String pin;
}
