package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @NotBlank(message = "Nome da empresa é obrigatório")
    @Size(max = 255, message = "Nome da empresa deve ter no máximo 255 caracteres")
    @Column(name = "name", nullable = false)
    private String name;
    
    @Size(max = 20, message = "CNPJ deve ter no máximo 20 caracteres")
    @Column(name = "document")
    private String document;
    
    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    @Column(name = "address")
    private String address;
    
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(name = "phone")
    private String phone;
    
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    @Column(name = "email")
    private String email;
    
    @Size(max = 255, message = "Website deve ter no máximo 255 caracteres")
    @Column(name = "website")
    private String website;
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}