package com.platform.app.iam.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.LoginRequest;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.RegisterRequest;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.repository.SpringDataUserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private EntityManager entityManager;
  @Autowired private SpringDataUserRepository springDataUserRepository;
  @Autowired private PasswordEncoder passwordEncoder;
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
        PermissionJpaEntity.builder()
            .id(UUID.randomUUID())
            .code("DOC_READ")
            .name("Read Documents")
            .module("DOC")
            .description("Read permission")
            .createdAt(Instant.now())
            .build();
    entityManager.persist(docRead);

    RoleJpaEntity userRole =
        RoleJpaEntity.builder()
            .id(UUID.randomUUID())
            .code("STAFF")
            .name("Staff User")
            .description("Staff role")
            .createdAt(Instant.now())
            .permissions(Set.of(docRead))
            .build();
    entityManager.persist(userRole);

    activeUserId = UUID.randomUUID();
    UserJpaEntity activeUser =
        UserJpaEntity.builder()
            .id(activeUserId)
            .email("active@platform.com")
            .passwordHash(passwordEncoder.encode("Password123#"))
            .fullName("Active User")
            .departmentId(null)
            .enabled(true)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(userRole))
            .build();
    entityManager.persist(activeUser);

    disabledUserId = UUID.randomUUID();
    UserJpaEntity disabledUser =
        UserJpaEntity.builder()
            .id(disabledUserId)
            .email("disabled@platform.com")
            .passwordHash(passwordEncoder.encode("Password123#"))
            .fullName("Disabled User")
            .departmentId(null)
            .enabled(false)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(userRole))
            .build();
    entityManager.persist(disabledUser);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("POST /api/v1/auth/login - 200 OK with tokens on valid credentials")
  void shouldLoginSuccessfully() throws Exception {
    LoginRequest request =
        LoginRequest.builder().email("active@platform.com").password("Password123#").build();

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
    LoginRequest request =
        LoginRequest.builder().email("not-an-email").password("Password123#").build();

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
    LoginRequest request =
        LoginRequest.builder().email("active@platform.com").password("").build();

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
    LoginRequest request =
        LoginRequest.builder().email("active@platform.com").password("WrongPassword999!").build();

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
    LoginRequest request =
        LoginRequest.builder().email("unknown@platform.com").password("SomePassword123#").build();

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
    LoginRequest request =
        LoginRequest.builder().email("disabled@platform.com").password("Password123#").build();

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
    LoginRequest badRequest =
        LoginRequest.builder().email("active@platform.com").password("WrongPassword!").build();

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
    LoginRequest correctRequest =
        LoginRequest.builder().email("active@platform.com").password("Password123#").build();
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

  @Test
  @DisplayName("POST /api/v1/auth/register - 201 Created on valid registration")
  void shouldRegisterSuccessfully() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .email("newuser@platform.com")
            .password("SecurePassword123#")
            .firstName("Jane")
            .lastName("Doe")
            .build();

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.email", is("newuser@platform.com")))
        .andExpect(jsonPath("$.fullName", is("Jane Doe")))
        .andExpect(jsonPath("$.roles", hasItem("STAFF")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/register - 409 Conflict when email already exists")
  void shouldReturn409WhenEmailAlreadyExists() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .email("active@platform.com")
            .password("SecurePassword123#")
            .firstName("Duplicate")
            .lastName("User")
            .build();

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status", is(409)))
        .andExpect(jsonPath("$.message", containsString("already registered")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/register - 400 Bad Request on invalid email")
  void shouldReturn400OnRegisterWithInvalidEmail() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .email("not-an-email")
            .password("SecurePassword123#")
            .firstName("Jane")
            .lastName("Doe")
            .build();

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("Invalid email format")));
  }

  @Test
  @DisplayName("POST /api/v1/auth/register - 400 Bad Request on blank first name")
  void shouldReturn400OnRegisterWithBlankFirstName() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .email("valid@platform.com")
            .password("SecurePassword123#")
            .firstName("")
            .lastName("Doe")
            .build();

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)))
        .andExpect(jsonPath("$.message", containsString("First name must not be blank")));
  }
}
