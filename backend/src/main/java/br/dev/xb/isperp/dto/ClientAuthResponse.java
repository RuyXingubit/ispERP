package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAuthResponse {

    // Statuses: AUTHENTICATED, PIN_REQUIRED, FORCE_CHANGE_PIN
    private String status;
    private String message;
    private UUID customerId;
    private String customerName;
    private String maskedDocument;
    private Boolean hasPin;
    private Customer customer;
}
