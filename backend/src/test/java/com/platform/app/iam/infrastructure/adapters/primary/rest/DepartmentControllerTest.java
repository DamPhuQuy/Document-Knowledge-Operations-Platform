package com.platform.app.iam.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.CreateDepartmentRequest;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.UpdateDepartmentRequest;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.DepartmentJpaEntity;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepartmentControllerTest {

  @Autowired private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private EntityManager entityManager;

  private UUID hrDeptId;
  private UUID itDeptId;

  @BeforeEach
  void setUp() {
    hrDeptId = UUID.randomUUID();
    DepartmentJpaEntity hr =
        DepartmentJpaEntity.builder()
            .id(hrDeptId)
            .code("HR")
            .name("Human Resources")
            .description("HR Team")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    entityManager.persist(hr);

    itDeptId = UUID.randomUUID();
    DepartmentJpaEntity it =
        DepartmentJpaEntity.builder()
            .id(itDeptId)
            .code("IT")
            .name("Information Technology")
            .description("IT Team")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    entityManager.persist(it);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/v1/departments - 201 Created on valid department")
  void createDepartment_Success() throws Exception {
    CreateDepartmentRequest request =
        new CreateDepartmentRequest("FIN", "Finance", "Finance and accounting");

    mockMvc
        .perform(
            post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code", is("FIN")))
        .andExpect(jsonPath("$.name", is("Finance")))
        .andExpect(jsonPath("$.description", is("Finance and accounting")))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/v1/departments - 400 Bad Request on invalid uppercase alphanumeric code (Rule B2)")
  void createDepartment_InvalidCode() throws Exception {
    CreateDepartmentRequest request =
        new CreateDepartmentRequest("invalid-code!", "Invalid Department", "Description");

    mockMvc
        .perform(
            post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST /api/v1/departments - 409 Conflict on duplicate department code (Path 2a)")
  void createDepartment_DuplicateCode() throws Exception {
    CreateDepartmentRequest request =
        new CreateDepartmentRequest("HR", "Another HR", "Duplicate code");

    mockMvc
        .perform(
            post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status", is(409)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/departments/{id} - 200 OK on successful update")
  void updateDepartment_Success() throws Exception {
    UpdateDepartmentRequest request =
        new UpdateDepartmentRequest("PEOPLE", "People & Culture", "Updated description");

    mockMvc
        .perform(
            put("/api/v1/departments/{id}", hrDeptId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is("PEOPLE")))
        .andExpect(jsonPath("$.name", is("People & Culture")))
        .andExpect(jsonPath("$.description", is("Updated description")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/departments/{id} - 409 Conflict when updating to another existing code")
  void updateDepartment_DuplicateCode() throws Exception {
    UpdateDepartmentRequest request =
        new UpdateDepartmentRequest("IT", "Try renaming HR to IT", "Conflict");

    mockMvc
        .perform(
            put("/api/v1/departments/{id}", hrDeptId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status", is(409)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/v1/departments/{id} - 404 Not Found on non-existent department")
  void updateDepartment_NotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    UpdateDepartmentRequest request =
        new UpdateDepartmentRequest("NEW_DEPT", "New Dept", "Desc");

    mockMvc
        .perform(
            put("/api/v1/departments/{id}", randomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/v1/departments/{id} - 200 OK on existing department")
  void getDepartmentById_Success() throws Exception {
    mockMvc
        .perform(get("/api/v1/departments/{id}", hrDeptId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(hrDeptId.toString())))
        .andExpect(jsonPath("$.code", is("HR")))
        .andExpect(jsonPath("$.name", is("Human Resources")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/v1/departments/{id} - 404 Not Found on missing department")
  void getDepartmentById_NotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/departments/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/v1/departments - 200 OK returns list")
  void listDepartments_Success() throws Exception {
    mockMvc
        .perform(get("/api/v1/departments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("Should return 403 Forbidden when invoked without ROLE_ADMIN")
  void nonAdminAccess_Forbidden() throws Exception {
    CreateDepartmentRequest request =
        new CreateDepartmentRequest("LEGAL", "Legal", "Legal dept");

    mockMvc
        .perform(
            post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should return 401/403 when unauthenticated")
  void unauthenticatedAccess_Rejected() throws Exception {
    mockMvc.perform(get("/api/v1/departments")).andExpect(status().isForbidden());
  }
}
