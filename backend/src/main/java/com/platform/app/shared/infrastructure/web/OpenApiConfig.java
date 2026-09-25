package com.platform.app.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

  public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

  @Bean
  public OpenAPI customOpenAPI(
      @Value("${app.swagger.title:Document and Knowledge Operations Platform API}") String title,
      @Value(
              "${app.swagger.description:RESTful API documentation for Document and Knowledge Operations Platform}")
          String description,
      @Value("${app.swagger.version:1.0.0}") String version) {
    return new OpenAPI()
        .info(
            new Info()
                .title(title)
                .version(version)
                .license(
                    new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token to authorize API testing.")));
  }
}
