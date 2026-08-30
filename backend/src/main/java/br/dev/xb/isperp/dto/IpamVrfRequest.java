package br.dev.xb.isperp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class IpamVrfRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O nome da VRF é obrigatório")
    private String name;

    private @Nullable String rd;
    private @Nullable String description;

    @JsonProperty("isDefault")
    @JsonAlias({"isDefault", "default"})
    @Builder.Default
    private boolean isDefault = false;
}
