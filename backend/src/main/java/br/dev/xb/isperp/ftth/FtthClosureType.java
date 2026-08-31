package br.dev.xb.isperp.ftth;

public enum FtthClosureType {
    DOMO("Caixa de Emenda Tipo Domo / Cilíndrica"),
    RETANGULAR("Caixa de Emenda Tipo Retangular / Horizontal"),
    SUBTERRANEA("Caixa de Emenda Subterrânea / Galeria");

    private final String description;

    FtthClosureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
