package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FtthSplitterType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthSplitterRequest {
    private @Nullable UUID companyId;
    private @Nullable UUID closureId;

    @NotBlank(message = "O nome do splitter é obrigatório")
    private String name;

    @Builder.Default
    private FtthSplitterType splitterType = FtthSplitterType.BALANCED_1_8;

    private @Nullable UUID inputCableId;
    private @Nullable Integer inputFiberNumber;

    private @Nullable BigDecimal attenuationDb;
}
