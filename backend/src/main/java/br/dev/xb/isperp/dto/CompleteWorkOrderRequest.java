package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteWorkOrderRequest {

    @NotBlank(message = "Endereço MAC da ONU é obrigatório")
    private String onuMac;

    @NotBlank(message = "Número de série da ONU é obrigatório")
    private String onuSerial;

    @NotNull(message = "Sinal óptico em dBm é obrigatório")
    private BigDecimal fiberSignalDbm;

    private BigDecimal technicianLatitude;
    private BigDecimal technicianLongitude;
    private String installationPhotoUrl;

    private String notes;
}
