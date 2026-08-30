package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusSessionResponse {
    private Long radacctId;
    private String acctSessionId;
    private String username;
    private String nasIpAddress;
    private @Nullable String nasShortname;
    private @Nullable OffsetDateTime acctStartTime;
    private @Nullable OffsetDateTime acctUpdateTime;
    private @Nullable OffsetDateTime acctStopTime;
    private @Nullable Integer acctSessionTime; // em segundos
    private Long acctInputOctets;  // Bytes Download
    private Long acctOutputOctets; // Bytes Upload
    private @Nullable String callingStationId; // MAC da ONT / CPE
    private @Nullable String framedIpAddress;
    private @Nullable String framedIpv6Prefix;
    private @Nullable String delegatedIpv6Prefix;
    private boolean isOnline;
    private @Nullable String customerName;
    private @Nullable String customerCpfCnpj;
}
