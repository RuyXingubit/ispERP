package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.signature.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplateRequest {

    private @Nullable UUID companyId;

    @NotBlank(message = "Nome do template é obrigatório")
    private String name;

    @NotNull(message = "Tipo de documento é obrigatório")
    private DocumentType documentType;

    private @Nullable Integer version;

    private @Nullable Boolean isActive;

    @NotBlank(message = "Conteúdo em Markdown é obrigatório")
    private String contentMarkdown;

    @NotBlank(message = "Cláusula de consentimento é obrigatória")
    private String consentClause;
}
