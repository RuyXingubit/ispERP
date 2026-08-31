package br.dev.xb.isperp.ftth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiberColorInfo {
    private int fiberNumber;
    private int tubeNumber;
    private int fiberInTubeNumber; // Posição 1 a 12 dentro do tubo
    private String fiberColorName;
    private String fiberColorHex;
    private String tubeColorName;
    private String tubeColorHex;
    private FiberColorStandard standard;
}
