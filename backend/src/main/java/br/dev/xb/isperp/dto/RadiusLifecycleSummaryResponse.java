package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusLifecycleSummaryResponse {

    private long totalPppoeUsers;
    private long totalActiveUsers;
    private long totalBlockedUsers;
    private long totalTrustUnblocked;
    private long todayAutoBlocksCount;
    private long todayUnblocksCount;
    private int toleranceDays;
    private boolean autoBlockEnabled;
}
