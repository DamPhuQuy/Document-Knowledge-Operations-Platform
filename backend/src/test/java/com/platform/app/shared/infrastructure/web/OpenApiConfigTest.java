package com.platform.app.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

  private final OpenApiConfig openApiConfig = new OpenApiConfig();

  @Test
  @DisplayName("Should configure OpenAPI with Bearer JWT SecurityScheme and SecurityRequirement")
  void shouldConfigureOpenApiWithBearerAuth() {
    OpenAPI openAPI =
        openApiConfig.customOpenAPI(
            "Test API", "Test Description", "1.0.0");

    assertThat(openAPI).isNotNull();
    assertThat(openAPI.getInfo().getTitle()).isEqualTo("Test API");
    assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");

    assertThat(openAPI.getComponents()).isNotNull();
    assertThat(openAPI.getComponents().getSecuritySchemes())
        .containsKey(OpenApiConfig.SECURITY_SCHEME_NAME);

    SecurityScheme scheme =
        openAPI.getComponents().getSecuritySchemes().get(OpenApiConfig.SECURITY_SCHEME_NAME);
    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
    assertThat(scheme.getScheme()).isEqualTo("bearer");
    assertThat(scheme.getBearerFormat()).isEqualTo("JWT");

    assertThat(openAPI.getSecurity()).isNotEmpty();
    assertThat(openAPI.getSecurity().get(0))
        .containsKey(OpenApiConfig.SECURITY_SCHEME_NAME);
  }
}
