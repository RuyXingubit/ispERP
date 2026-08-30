package br.dev.xb.isperp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "radacct")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadAcct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "radacctid")
    private Long radacctId;

    @Column(name = "acctsessionid", nullable = false, length = 64)
    private String acctSessionId;

    @Column(name = "acctuniqueid", nullable = false, unique = true, length = 32)
    private String acctUniqueId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(length = 64)
    @Builder.Default
    private String realm = "";

    @Column(name = "nasipaddress", nullable = false, length = 45)
    private String nasIpAddress;

    @Column(name = "nasportid", length = 32)
    private @Nullable String nasPortId;

    @Column(name = "nasporttype", length = 32)
    private @Nullable String nasPortType;

    @Column(name = "acctstarttime")
    private @Nullable OffsetDateTime acctStartTime;

    @Column(name = "acctupdatetime")
    private @Nullable OffsetDateTime acctUpdateTime;

    @Column(name = "acctstoptime")
    private @Nullable OffsetDateTime acctStopTime;

    @Column(name = "acctinterval")
    private @Nullable Integer acctInterval;

    @Column(name = "acctsessiontime")
    private @Nullable Integer acctSessionTime;

    @Column(name = "acctauthentic", length = 32)
    private @Nullable String acctAuthentic;

    @Column(name = "connectinfo_start", length = 128)
    private @Nullable String connectInfoStart;

    @Column(name = "connectinfo_stop", length = 128)
    private @Nullable String connectInfoStop;

    @Column(name = "acctinputoctets")
    @Builder.Default
    private Long acctInputOctets = 0L;

    @Column(name = "acctoutputoctets")
    @Builder.Default
    private Long acctOutputOctets = 0L;

    @Column(name = "calledstationid", length = 50)
    private @Nullable String calledStationId;

    @Column(name = "callingstationid", length = 50)
    private @Nullable String callingStationId; // MAC da ONT / CPE

    @Column(name = "acctterminatecause", length = 32)
    private @Nullable String acctTerminateCause;

    @Column(name = "servicetype", length = 32)
    private @Nullable String serviceType;

    @Column(name = "framedprotocol", length = 32)
    private @Nullable String framedProtocol;

    @Column(name = "framedipaddress", length = 45)
    private @Nullable String framedIpAddress;

    @Column(name = "framedipv6prefix", length = 45)
    private @Nullable String framedIpv6Prefix;

    @Column(name = "delegatedipv6prefix", length = 45)
    private @Nullable String delegatedIpv6Prefix;
}
