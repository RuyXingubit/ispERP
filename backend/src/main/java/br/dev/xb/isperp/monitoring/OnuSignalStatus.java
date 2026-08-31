package br.dev.xb.isperp.monitoring;

public enum OnuSignalStatus {
    ONLINE_GOOD,       // -15 dBm a -24 dBm
    ONLINE_WARNING,    // -24.01 dBm a -27 dBm
    ONLINE_CRITICAL,   // pior que -27 dBm
    LOS,               // Loss of Signal (Sem luz)
    DYING_GASP,        // Falta de energia na ONU
    OFFLINE            // Desconectado
}
