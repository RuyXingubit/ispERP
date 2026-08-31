package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthFeasibilityResponse {
    private boolean viable;
    private int viableCtosCount;
    private List<FeasibleCtoItem> nearbyCtos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeasibleCtoItem {
        private FtthCtoResponse cto;
        private double distanceMeters;
        private int freePorts;
        private boolean hasCapacity;
    }
}
