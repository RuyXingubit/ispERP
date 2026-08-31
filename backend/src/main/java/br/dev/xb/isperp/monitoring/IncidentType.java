package br.dev.xb.isperp.monitoring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentType {
    FIBER_CUT_PROBABLE("Rompimento Provável de Fibra Óptica"),
    POWER_OUTAGE_PROBABLE("Queda de Energia Elétrica / Apagão no Bairro"),
    MASSIVE_LOS_PON("Queda Massiva de Sinal na Porta PON"),
    CTO_OFFLINE("CTO Completamente Inoperante"),
    DEGRADED_SIGNAL("Degradação de Atenuação Óptica");

    private final String description;
}
