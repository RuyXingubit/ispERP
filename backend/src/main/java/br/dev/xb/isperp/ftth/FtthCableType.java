package br.dev.xb.isperp.ftth;

public enum FtthCableType {
    ALIMENTADOR("Cabo Alimentador / Troncal"),
    DISTRIBUICAO("Cabo de Distribuição"),
    DROP("Cabo Drop / Atendimento");

    private final String description;

    FtthCableType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
