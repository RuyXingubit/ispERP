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
public class NfcomStatusResult {
    private boolean success;
    @Nullable
    private String chaveAcesso;
    @Nullable
    private String status;
    @Nullable
    private String protocoloAutorizacao;
    @Nullable
    private LocalDateTime dataAutorizacao;
    @Nullable
    private String danfePdfUrl;
    @Nullable
    private String xmlUrl;
    @Nullable
    private String errorMessage;
}
