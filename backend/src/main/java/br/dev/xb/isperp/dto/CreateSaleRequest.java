package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleRequest {

    @NotNull(message = "ID do plano é obrigatório")
    private UUID planId;

    @NotBlank(message = "Nome do cliente é obrigatório")
    private String customerName;

    @NotBlank(message = "CPF do cliente é obrigatório")
    private String customerCpf;

    private String customerEmail;

    @NotBlank(message = "Telefone do cliente é obrigatório")
    private String customerPhone;

    @NotBlank(message = "Endereço de instalação é obrigatório")
    private String installationAddress;

    @NotBlank(message = "Cidade é obrigatória")
    private String city;

    @NotBlank(message = "Estado é obrigatório")
    private String state;

    @NotBlank(message = "CEP é obrigatório")
    private String zipCode;

    @Builder.Default
    private Integer preferredDueDate = 10;

    @Builder.Default
    private String notificationChannel = "WHATSAPP";

    private String sellerName;
}
