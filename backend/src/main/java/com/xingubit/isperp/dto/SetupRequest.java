package com.xingubit.isperp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetupRequest {
    
    // Dados do usuário administrador
    @NotBlank(message = "Nome do administrador é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String adminName;
    
    @NotBlank(message = "Email do administrador é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    private String adminEmail;
    
    @NotBlank(message = "Senha do administrador é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String adminPassword;
    
    // Dados da empresa
    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(max = 255, message = "Nome da empresa deve ter no máximo 255 caracteres")
    private String companyName;
    
    @Size(max = 20, message = "CNPJ deve ter no máximo 20 caracteres")
    private String companyCnpj;
    
    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    private String companyAddress;
    
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String companyPhone;
    
    @Size(max = 255, message = "Email da empresa deve ter no máximo 255 caracteres")
    private String companyEmail;
    
    @Size(max = 255, message = "Website deve ter no máximo 255 caracteres")
    private String companyWebsite;
    
    // Configurações do site
    @NotBlank(message = "Título do site é obrigatório")
    @Size(max = 255, message = "Título do site deve ter no máximo 255 caracteres")
    private String siteTitle;
    
    @Size(max = 500, message = "Descrição do site deve ter no máximo 500 caracteres")
    private String siteDescription;
    
    @Size(max = 7, message = "Cor primária deve ter no máximo 7 caracteres")
    private String primaryColor;
    
    @Size(max = 7, message = "Cor secundária deve ter no máximo 7 caracteres")
    private String secondaryColor;
}