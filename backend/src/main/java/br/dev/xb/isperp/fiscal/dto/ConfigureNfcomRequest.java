package br.dev.xb.isperp.fiscal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigureNfcomRequest {
    private String cnpj;
    private String ambiente; // "homologacao" ou "producao"
    private String serie;
    private Integer proximoNumero;
}
