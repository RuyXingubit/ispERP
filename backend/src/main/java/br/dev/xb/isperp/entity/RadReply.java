package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "radreply")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadReply {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 253)
    private String value;

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String op = "=";

    @Column(nullable = false, length = 64)
    private String attribute;

    @Column(nullable = false, length = 64)
    private String username;

    @PrePersist
    public void prePersist() {
        this.id = UuidCreatorUtils.orGenerate(this.id);
    }
}
