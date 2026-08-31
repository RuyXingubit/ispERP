package br.dev.xb.isperp.ftth;

public enum FtthSplitterType {
    // Balanceados PLC
    BALANCED_1_2("Splitter Balanceado 1:2", 3.50, 2),
    BALANCED_1_4("Splitter Balanceado 1:4", 7.20, 4),
    BALANCED_1_8("Splitter Balanceado 1:8", 10.50, 8),
    BALANCED_1_16("Splitter Balanceado 1:16", 13.80, 16),
    BALANCED_1_32("Splitter Balanceado 1:32", 17.50, 32),
    BALANCED_1_64("Splitter Balanceado 1:64", 21.00, 64),

    // Desbalanceados FBT (Barramento)
    UNBALANCED_95_05("Splitter Desbalanceado 95/05", 13.50, 2),
    UNBALANCED_90_10("Splitter Desbalanceado 90/10", 10.50, 2),
    UNBALANCED_85_15("Splitter Desbalanceado 85/15", 8.80, 2),
    UNBALANCED_80_20("Splitter Desbalanceado 80/20", 7.40, 2),
    UNBALANCED_75_25("Splitter Desbalanceado 75/25", 6.50, 2),
    UNBALANCED_70_30("Splitter Desbalanceado 70/30", 5.70, 2),
    UNBALANCED_60_40("Splitter Desbalanceado 60/40", 4.40, 2),
    UNBALANCED_50_50("Splitter Desbalanceado 50/50", 3.50, 2);

    private final String description;
    private final double defaultAttenuationDb;
    private final int outputPorts;

    FtthSplitterType(String description, double defaultAttenuationDb, int outputPorts) {
        this.description = description;
        this.defaultAttenuationDb = defaultAttenuationDb;
        this.outputPorts = outputPorts;
    }

    public String getDescription() {
        return description;
    }

    public double getDefaultAttenuationDb() {
        return defaultAttenuationDb;
    }

    public int getOutputPorts() {
        return outputPorts;
    }
}
