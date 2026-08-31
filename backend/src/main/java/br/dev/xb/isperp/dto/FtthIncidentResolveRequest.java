package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthIncidentResolveRequest {
    @NotBlank(message = "As notas de resolução / causa raiz são obrigatórias")
    private String rootCauseNotes;
}
