package br.dev.xb.isperp.dto;

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
public class FtthPopRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O nome do POP é obrigatório")
    private String name;

    private @Nullable BigDecimal latitude;
    private @Nullable BigDecimal longitude;
    private @Nullable String address;
    private @Nullable String description;
}
