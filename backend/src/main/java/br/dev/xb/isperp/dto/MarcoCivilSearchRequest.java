package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarcoCivilSearchRequest {
    @NotBlank(message = "O endereço IP pesquisado é obrigatório (Público, Privado ou IPv6)")
    private String ip;

    private @Nullable Integer port;

    @NotNull(message = "A data e hora do evento investigado são obrigatórias")
    private OffsetDateTime timestamp;
}
