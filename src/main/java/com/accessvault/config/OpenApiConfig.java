package com.accessvault.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI accessVaultOpenAPI() {
    final String bearerKey = "bearerAuth";
    return new OpenAPI()
        .info(new Info()
            .title("AccessVault API")
            .description("Secure secrets vault with JWT, RBAC, rate limiting, and audit logging.")
            .version("v1")
            .contact(new Contact().name("AccessVault").email("noreply@example.com")))
        .addSecurityItem(new SecurityRequirement().addList(bearerKey))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes(bearerKey, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")));
  }
}
