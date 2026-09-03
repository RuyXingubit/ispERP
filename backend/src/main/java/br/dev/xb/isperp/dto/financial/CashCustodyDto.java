package br.dev.xb.isperp.dto.financial;

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
public class CashCustodyDto {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private String cpf;
    private BigDecimal currentBalance;
    private OffsetDateTime updatedAt;
}
