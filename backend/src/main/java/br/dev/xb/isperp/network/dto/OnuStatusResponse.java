package br.dev.xb.isperp.network.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnuStatusResponse {

    private String onuMac;
    private String onuSerial;
    private String status; // ONLINE, OFFLINE, BLOCKED
    private BigDecimal rxPowerDbm; // Sinal óptico
    private BigDecimal txPowerDbm;
    private String oltName;
    private Integer ponPort;
    private String details;
}
