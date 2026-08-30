package br.dev.xb.isperp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "ispERP - API Documentation",
        version = "1.0.0",
        description = "Documentação oficial dos endpoints REST do ispERP (Sistema ERP Modular para Provedores de Internet - Java 25, Spring Boot 4.1.1, PostgreSQL 17 com UUIDv7).",
        contact = @Contact(
            name = "Equipe ispERP",
            url = "https://github.com/RuyXingubit/ispERP"
        ),
        license = @License(
            name = "GPL-3.0",
            url = "https://www.gnu.org/licenses/gpl-3.0.html"
        )
    ),
    security = {
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Insira o token JWT gerado no endpoint /auth/login para autenticar as requisições protegidas."
)
public class OpenApiConfig {
}
