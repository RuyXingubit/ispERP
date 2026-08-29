package com.xingubit.isperp.entity;

import com.xingubit.isperp.util.UuidCreatorUtils;
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
@Table(name = "site_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettings {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @NotBlank(message = "Título do site é obrigatório")
    @Size(max = 255, message = "Título do site deve ter no máximo 255 caracteres")
    @Column(name = "site_title", nullable = false)
    private String siteTitle;
    
    @Size(max = 500, message = "Descrição do site deve ter no máximo 500 caracteres")
    @Column(name = "site_description")
    private String siteDescription;
    
    @Size(max = 7, message = "Cor primária deve ter no máximo 7 caracteres")
    @Column(name = "primary_color")
    @Builder.Default
    private String primaryColor = "#1976d2";
    
    @Size(max = 7, message = "Cor secundária deve ter no máximo 7 caracteres")
    @Column(name = "secondary_color")
    @Builder.Default
    private String secondaryColor = "#dc004e";
    
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