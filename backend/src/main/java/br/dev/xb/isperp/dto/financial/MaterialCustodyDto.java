package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCustodyDto {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userCpf;
    private String itemName;
    private MaterialType itemType;
    private String serialNumber;
    private String macAddress;
    private BigDecimal quantity;
    private String unit;
    private OffsetDateTime allocatedAt;
    private String notes;
}
