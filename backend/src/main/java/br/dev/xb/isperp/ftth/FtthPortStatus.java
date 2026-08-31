package br.dev.xb.isperp.ftth;

public enum FtthPortStatus {
    LIVRE("Porta Livre para Venda"),
    OCUPADA("Porta Ocupada com Assinante"),
    RESERVADA("Porta Reservada para Instalação"),
    DEFEITO("Porta com Defeito / Atenuação");

    private final String description;

    FtthPortStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
