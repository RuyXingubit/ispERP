package br.dev.xb.isperp.fiscal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfcomIssueResult {
    private boolean success;
    @Nullable
    private String externalId;
    @Nullable
    private String chaveAcesso;
    @Nullable
    private Integer numero;
    @Nullable
    private String serie;
    @Nullable
    private String status; // "AUTORIZADA", "PROCESSANDO", "REJEITADA", "ERRO"
    @Nullable
    private String protocoloAutorizacao;
    @Nullable
    private LocalDateTime dataAutorizacao;
    @Nullable
    private String digestValue;
    @Nullable
    private String danfePdfUrl;
    @Nullable
    private String xmlUrl;
    @Nullable
    private String errorMessage;
}
