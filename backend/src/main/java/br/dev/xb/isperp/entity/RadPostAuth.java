package br.dev.xb.isperp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "radpostauth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadPostAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(length = 64)
    private @Nullable String pass;

    @Column(length = 32)
    private @Nullable String reply;

    @Column(name = "calledstationid", length = 50)
    private @Nullable String calledStationId;

    @Column(name = "callingstationid", length = 50)
    private @Nullable String callingStationId;

    @Column(name = "authdate", nullable = false)
    @Builder.Default
    private OffsetDateTime authDate = OffsetDateTime.now();

    @Column(name = "class", length = 64)
    private @Nullable String acctClass;
}
