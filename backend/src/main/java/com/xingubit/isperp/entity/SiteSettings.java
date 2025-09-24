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
@Table(name = "site_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Título do site é obrigatório")
    @Size(max = 255, message = "Título do site deve ter no máximo 255 caracteres")
    @Column(name = "site_title", nullable = false)
    private String siteTitle;
    
    @Size(max = 500, message = "Descrição do site deve ter no máximo 500 caracteres")
    @Column(name = "site_description")
    private String siteDescription;
    
    @Size(max = 7, message = "Cor primária deve ter no máximo 7 caracteres")
    @Column(name = "primary_color")
    private String primaryColor = "#1976d2";
    
    @Size(max = 7, message = "Cor secundária deve ter no máximo 7 caracteres")
    @Column(name = "secondary_color")
    private String secondaryColor = "#dc004e";
    
    @Size(max = 255, message = "Logo deve ter no máximo 255 caracteres")
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Size(max = 255, message = "Favicon deve ter no máximo 255 caracteres")
    @Column(name = "favicon_url")
    private String faviconUrl;
    
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