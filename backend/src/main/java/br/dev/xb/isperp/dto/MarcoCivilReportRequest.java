package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcoCivilReportRequest {
    private @Nullable String courtOrderNumber; // Nº do Ofício ou Inquérito Policial
    private @Nullable String requesterAuthority; // Delegacia / Juizado solicitante

    @NotBlank(message = "O IP pesquisado é obrigatório")
    private String queriedIp;

    private @Nullable Integer queriedPort;

    @NotNull(message = "A data/hora do evento é obrigatória")
    private OffsetDateTime queriedTimestamp;

    private @Nullable UUID matchedContractId;
    private @Nullable String notes;
}
