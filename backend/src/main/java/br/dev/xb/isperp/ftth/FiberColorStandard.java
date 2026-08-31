package br.dev.xb.isperp.ftth;

public enum FiberColorStandard {
    ABNT_NBR_14106("Padrão Nacional ABNT NBR 14106 / Telebrás"),
    TIA_EIA_598("Padrão Internacional TIA/EIA-598");

    private final String description;

    FiberColorStandard(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
