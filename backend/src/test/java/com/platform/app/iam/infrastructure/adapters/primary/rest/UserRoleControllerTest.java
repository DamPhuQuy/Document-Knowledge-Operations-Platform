package com.platform.app.iam.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.request.AssignRolesRequest;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.PermissionJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.RoleJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EntityManager entityManager;

    private UUID adminUserId;
    private UUID staffUserId;
    private UUID adminRoleId;
    private UUID managerRoleId;
    private UUID staffRoleId;

    @BeforeEach
    void setUp() {
        PermissionJpaEntity manageUsersPerm = PermissionJpaEntity.builder()
            .id(UUID.randomUUID())
            .code("MANAGE:USERS")
            .name("Manage Users")
            .module("IAM")
            .createdAt(Instant.now())
            .build();
        entityManager.persist(manageUsersPerm);

        adminRoleId = UUID.randomUUID();
        RoleJpaEntity adminRole = RoleJpaEntity.builder()
            .id(adminRoleId)
            .code("ROLE_ADMIN")
            .name("Administrator")
            .createdAt(Instant.now())
            .permissions(Set.of(manageUsersPerm))
            .build();
        entityManager.persist(adminRole);

        managerRoleId = UUID.randomUUID();
        RoleJpaEntity managerRole = RoleJpaEntity.builder()
            .id(managerRoleId)
            .code("ROLE_MANAGER")
            .name("Department Manager")
            .createdAt(Instant.now())
            .permissions(Set.of())
            .build();
        entityManager.persist(managerRole);

        staffRoleId = UUID.randomUUID();
        RoleJpaEntity staffRole = RoleJpaEntity.builder()
            .id(staffRoleId)
            .code("ROLE_STAFF")
            .name("Staff")
            .createdAt(Instant.now())
            .permissions(Set.of())
            .build();
        entityManager.persist(staffRole);

        adminUserId = UUID.randomUUID();
        UserJpaEntity adminUser = UserJpaEntity.builder()
            .id(adminUserId)
            .email("admin@platform.com")
            .passwordHash("hashed")
            .fullName("Admin User")
            .enabled(true)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(adminRole))
            .build();
        entityManager.persist(adminUser);

        staffUserId = UUID.randomUUID();
        UserJpaEntity staffUser = UserJpaEntity.builder()
            .id(staffUserId)
            .email("staff@platform.com")
            .passwordHash("hashed")
            .fullName("Staff User")
            .enabled(true)
            .isInternal(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .roles(Set.of(staffRole))
            .build();
        entityManager.persist(staffUser);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN", "manage:users" })
    @DisplayName("Should return 200 and roles when queried by administrator")
    void shouldGetUserRolesAsAdmin() throws Exception {
        mockMvc
            .perform(get("/api/v1/users/{userId}/roles", staffUserId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId", is(staffUserId.toString())))
            .andExpect(jsonPath("$.email", is("staff@platform.com")))
            .andExpect(jsonPath("$.roles", hasSize(1)))
            .andExpect(jsonPath("$.roles[0].code", is("ROLE_STAFF")));
    }

    @Test
    @DisplayName("Should return 401/403 when unauthenticated")
    void shouldRejectUnauthenticated() throws Exception {
        mockMvc
            .perform(get("/api/v1/users/{userId}/roles", staffUserId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_STAFF" })
    @DisplayName(
        "Should return 403 when user lacks manage:users and ROLE_ADMIN"
    )
    void shouldRejectUnauthorizedUser() throws Exception {
        mockMvc
            .perform(get("/api/v1/users/{userId}/roles", staffUserId))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN", "manage:users" })
    @DisplayName("Should assign multiple roles successfully")
    void shouldAssignRolesSuccessfully() throws Exception {
        AssignRolesRequest request = new AssignRolesRequest(
            Set.of(staffRoleId, managerRoleId)
        );

        mockMvc
            .perform(
                put("/api/v1/users/{userId}/roles", staffUserId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId", is(staffUserId.toString())))
            .andExpect(jsonPath("$.roles", hasSize(2)))
            .andExpect(jsonPath("$.roles[*].code", hasItem("ROLE_STAFF")))
            .andExpect(jsonPath("$.roles[*].code", hasItem("ROLE_MANAGER")));
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN", "manage:users" })
    @DisplayName("Should reject empty role set with 400 Bad Request")
    void shouldRejectEmptyRoleSet() throws Exception {
        AssignRolesRequest request = new AssignRolesRequest(Set.of());

        mockMvc
            .perform(
                put("/api/v1/users/{userId}/roles", staffUserId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(
        "Should reject admin revoking own ROLE_ADMIN with 400 Bad Request"
    )
    void shouldRejectAdminSelfRoleRevocation() throws Exception {
        AssignRolesRequest request = new AssignRolesRequest(
            Set.of(staffRoleId)
        );

        mockMvc
            .perform(
                put("/api/v1/users/{userId}/roles", adminUserId)
                    .with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(
                            adminUserId.toString()
                        )
                            .roles("ADMIN")
                            .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "manage:users"
                                ),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    "ROLE_ADMIN"
                                )
                            )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath(
                    "$.message",
                    org.hamcrest.Matchers.containsString(
                        "cannot revoke their own ROLE_ADMIN"
                    )
                )
            );
    }
}
