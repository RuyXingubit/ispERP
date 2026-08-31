package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.signature.DocumentType;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplateResponse {

    private UUID id;
    private @Nullable UUID companyId;
    private String name;
    private DocumentType documentType;
    private Integer version;
    private Boolean isActive;
    private String contentMarkdown;
    private String consentClause;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
