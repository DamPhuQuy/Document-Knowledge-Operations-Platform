package com.platform.app.iam.web.dto;

import com.platform.app.iam.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Schema(example = "John Doe")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(example = "Password123!")
    private String password;

    @Schema(example = "CUSTOMER", defaultValue = "CUSTOMER")
    private Role role;
}
