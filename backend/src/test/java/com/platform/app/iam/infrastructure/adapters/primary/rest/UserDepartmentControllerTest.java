package com.platform.app.iam.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.AssignUserDepartmentRequest;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.DepartmentJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserDepartmentControllerTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private EntityManager entityManager;

  private UUID targetUserId;
  private UUID hrDeptId;

  @BeforeEach
  void setUp() {
    hrDeptId = UUID.randomUUID();
    DepartmentJpaEntity hrDept =
        DepartmentJpaEntity.builder()
            .id(hrDeptId)
            .code("HR")
            .name("Human Resources")
            .description("HR Team")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    entityManager.persist(hrDept);

    UUID roleId = UUID.randomUUID();
    RoleJpaEntity role =
        RoleJpaEntity.builder()
            .id(roleId)
            .code("ROLE_STAFF")
            .name("Staff")
            .createdAt(Instant.now())
            .permissions(Set.of())
            .build();
    entityManager.persist(role);

    targetUserId = UUID.randomUUID();
    UserJpaEntity user =
        UserJpaEntity.builder()
            .id(targetUserId)
            .email("employee@platform.com")
            .passwordHash("hashed")
            .fullName("Target Employee")
            .departmentId(null)
            .enabled(true)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(role))
            .build();
    entityManager.persist(user);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/users/{userId}/department - 200 OK on assigning department and is_internal")
  void assignDepartment_Success() throws Exception {
    AssignUserDepartmentRequest request =
        new AssignUserDepartmentRequest(hrDeptId, false);

    mockMvc
        .perform(
            put("/api/v1/users/{userId}/department", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId", is(targetUserId.toString())))
        .andExpect(jsonPath("$.departmentId", is(hrDeptId.toString())))
        .andExpect(jsonPath("$.departmentCode", is("HR")))
        .andExpect(jsonPath("$.departmentName", is("Human Resources")))
        .andExpect(jsonPath("$.internal", is(false)));

    entityManager.flush();
    entityManager.clear();

    UserJpaEntity updatedUser = entityManager.find(UserJpaEntity.class, targetUserId);
    assertEquals(hrDeptId, updatedUser.getDepartmentId());
    assertFalse(updatedUser.isInternal());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/users/{userId}/department - 404 Not Found on non-existent department (NF1)")
  void assignDepartment_DepartmentNotFound() throws Exception {
    UUID nonExistentDeptId = UUID.randomUUID();
    AssignUserDepartmentRequest request =
        new AssignUserDepartmentRequest(nonExistentDeptId, true);

    mockMvc
        .perform(
            put("/api/v1/users/{userId}/department", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/users/{userId}/department - 200 OK when clearing department (departmentId = null)")
  void assignDepartment_ClearDepartment() throws Exception {
    AssignUserDepartmentRequest request =
        new AssignUserDepartmentRequest(null, true);

    mockMvc
        .perform(
            put("/api/v1/users/{userId}/department", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId", is(targetUserId.toString())))
        .andExpect(jsonPath("$.departmentId", nullValue()))
        .andExpect(jsonPath("$.internal", is(true)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/users/{userId}/department - 400 Bad Request when isInternal is null")
  void assignDepartment_MissingIsInternal() throws Exception {
    String invalidPayload = "{\"departmentId\": \"" + hrDeptId + "\", \"isInternal\": null}";

    mockMvc
        .perform(
            put("/api/v1/users/{userId}/department", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  @DisplayName("PUT /api/v1/users/{userId}/department - 403 Forbidden for non-admin")
  void assignDepartment_NonAdminForbidden() throws Exception {
    AssignUserDepartmentRequest request =
        new AssignUserDepartmentRequest(hrDeptId, false);

    mockMvc
        .perform(
            put("/api/v1/users/{userId}/department", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
