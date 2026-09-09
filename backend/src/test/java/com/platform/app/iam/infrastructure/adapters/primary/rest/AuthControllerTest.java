package com.platform.app.iam.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.iam.application.ports.outbound.PasswordEncoderPort;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.SpringDataUserRepository;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.UserJpaEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private EntityManager entityManager;
  @Autowired private SpringDataUserRepository springDataUserRepository;
  @Autowired private PasswordEncoderPort passwordEncoderPort;
  @Autowired private com.platform.app.iam.application.ports.outbound.AccountLockoutPort accountLockoutPort;

  private UUID activeUserId;
  private UUID disabledUserId;

  @BeforeEach
  void setUp() {
    accountLockoutPort.resetAttempts("active@platform.com");
    accountLockoutPort.resetAttempts("disabled@platform.com");
    accountLockoutPort.resetAttempts("unknown@platform.com");
    springDataUserRepository.deleteAll();

    PermissionJpaEntity docRead =
        new PermissionJpaEntity(
            UUID.randomUUID(), "DOC_READ", "Read Documents", "DOC", "Read permission", Instant.now());
    entityManager.persist(docRead);

    RoleJpaEntity userRole =
        new RoleJpaEntity(
            UUID.randomUUID(), "STAFF", "Staff User", "Staff role", Instant.now(), Set.of(docRead));
    entityManager.persist(userRole);

    activeUserId = UUID.randomUUID();
    UserJpaEntity activeUser =
        new UserJpaEntity(
            activeUserId,
            "active@platform.com",
            passwordEncoderPort.encode("Password123#"),
            "Active User",
            null,
            true,
            true,
            Instant.now(),
            Instant.now(),
            Set.of(userRole));
    entityManager.persist(activeUser);

    disabledUserId = UUID.randomUUID();
    UserJpaEntity disabledUser =
        new UserJpaEntity(
            disabledUserId,
            "disabled@platform.com",
            passwordEncoderPort.encode("Password123#"),
            "Disabled User",
            null,
            false,
            true,
            Instant.now(),
            Instant.now(),
            Set.of(userRole));
    entityManager.persist(disabledUser);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 200 OK with tokens on valid credentials")
  void shouldLoginSuccessfully() throws Exception {
    LoginRequest request = new LoginRequest("active@platform.com", "Password123#");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("User-Agent", "JUnit-Test-Agent")
                .header("X-Forwarded-For", "192.168.1.100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.refreshToken", notNullValue()))
        .andExpect(jsonPath("$.tokenType", is("Bearer")))
        .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
        .andExpect(jsonPath("$.user.id", is(activeUserId.toString())))
        .andExpect(jsonPath("$.user.email", is("active@platform.com")))
        .andExpect(jsonPath("$.user.fullName", is("Active User")))
        .andExpect(jsonPath("$.user.roles", hasItem("STAFF")))
        .andExpect(jsonPath("$.user.permissions", hasItem("DOC_READ")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 400 Bad Request on invalid email format")
  void shouldReturn400OnInvalidEmail() throws Exception {
    LoginRequest request = new LoginRequest("not-an-email", "Password123#");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("Invalid email format")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 400 Bad Request on blank password")
  void shouldReturn400OnBlankPassword() throws Exception {
    LoginRequest request = new LoginRequest("active@platform.com", "");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("Password must not be blank")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 401 Unauthorized on wrong password")
  void shouldReturn401OnWrongPassword() throws Exception {
    LoginRequest request = new LoginRequest("active@platform.com", "WrongPassword999!");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)))
        .andExpect(jsonPath("$.message", is("Invalid email or password")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 401 Unauthorized on non-existent user")
  void shouldReturn401OnUnknownUser() throws Exception {
    LoginRequest request = new LoginRequest("unknown@platform.com", "SomePassword123#");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)))
        .andExpect(jsonPath("$.message", is("Invalid email or password")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 403 Forbidden on disabled account")
  void shouldReturn403OnDisabledAccount() throws Exception {
    LoginRequest request = new LoginRequest("disabled@platform.com", "Password123#");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status", is(403)))
        .andExpect(jsonPath("$.message", is("Account is deactivated")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 423 Locked on 5 consecutive failed attempts")
  void shouldReturn423AfterFiveFailedAttempts() throws Exception {
    LoginRequest badRequest = new LoginRequest("active@platform.com", "WrongPassword!");

    // 4 failed attempts -> 401
    for (int i = 0; i < 4; i++) {
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(badRequest)))
          .andExpect(status().isUnauthorized());
    }

    // 5th failed attempt -> 401 (locks account for next attempt)
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)))
        .andExpect(status().isUnauthorized());

    // 6th attempt (even with correct password) -> 423 LOCKED
    LoginRequest correctRequest = new LoginRequest("active@platform.com", "Password123#");
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctRequest)))
        .andExpect(status().is(423))
        .andExpect(jsonPath("$.status", is(423)))
        .andExpect(
            jsonPath(
                "$.message",
                is(
                    "Account is temporarily locked due to 5 consecutive failed login attempts. Please try again after 15 minutes.")));
  }
}
