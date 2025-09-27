package com.xingubit.isperp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}