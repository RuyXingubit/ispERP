package br.dev.xb.isperp.network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnuProvisionRequest {

    private UUID contractId;
    private UUID customerId;
    private String customerName;
    private String onuMac;
    private String onuSerial;
    private Integer vlanId;
    private String pppoeUser;
    private String pppoePassword;
    private Integer downloadSpeed; // Mbps
    private Integer uploadSpeed;   // Mbps
    private BigDecimal rxPowerDbm;
}
