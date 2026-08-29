package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleWorkOrderRequest {

    @NotNull(message = "Data do agendamento é obrigatória")
    private LocalDate scheduledDate;

    @NotBlank(message = "Período do agendamento é obrigatório")
    private String scheduledPeriod; // MANHA, TARDE, NOITE, SABADO_MANHA

    @NotBlank(message = "Nome do técnico responsável é obrigatório")
    private String technicianName;

    private String notes;
}
