package br.dev.xb.isperp.fiscal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterCompanyRequest {
    private String cnpj;
    private String razaoSocial;
    @Nullable
    private String nomeFantasia;
    private String inscricaoEstadual;
    @Nullable
    private String inscricaoMunicipal;
    private String cnaePrincipal;
    private String regimeTributario;
    private String logradouro;
    private String numero;
    @Nullable
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;
    private String codigoIbge;
    @Nullable
    private String telefone;
    @Nullable
    private String emailFiscal;
}
