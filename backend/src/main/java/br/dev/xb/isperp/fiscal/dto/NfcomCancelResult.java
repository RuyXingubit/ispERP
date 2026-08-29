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
public class NfcomCancelResult {
    private boolean success;
    @Nullable
    private String chaveAcesso;
    @Nullable
    private String protocoloCancelamento;
    @Nullable
    private LocalDateTime dataCancelamento;
    @Nullable
    private String errorMessage;
}
