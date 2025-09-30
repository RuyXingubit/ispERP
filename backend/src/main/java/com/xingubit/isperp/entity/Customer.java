package com.xingubit.isperp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.xingubit.isperp.validation.ValidCpf;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    @Column(name = "name", nullable = false)
    private String name;
    
    @NotBlank(message = "CPF é obrigatório")
    @ValidCpf(message = "CPF inválido")
    @Column(name = "cpf", nullable = false, unique = true, length = 14)
    private String cpf;
    
    @Email(message = "Email deve ter formato válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    @Column(name = "email", unique = true)
    private String email;
    
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Size(max = 500, message = "Endereço deve ter no máximo 500 caracteres")
    @Column(name = "address", length = 500)
    private String address;
    
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    @Column(name = "city", length = 100)
    private String city;
    
    @Size(max = 2, message = "Estado deve ter no máximo 2 caracteres")
    @Column(name = "state", length = 2)
    private String state;
    
    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    @Pattern(regexp = "\\d{5}-\\d{3}|\\d{8}", message = "CEP deve ter formato válido")
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
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