package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.NasVendorType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CgnatScriptImportRequest {
    private @Nullable UUID nasId;

    @Builder.Default
    private NasVendorType vendorType = NasVendorType.MIKROTIK;

    @NotBlank(message = "O script ou CSV/texto com as regras é obrigatório")
    private String scriptContent;

    @Builder.Default
    private boolean replaceExisting = false;
}
