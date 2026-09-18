package com.platform.app.document.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.document.application.dto.UpdateDocumentPermissionsRequest;
import com.platform.app.document.application.dto.UserGrantDto;
import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.StorageException;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.DocumentStatus;
import com.platform.app.document.domain.model.PermissionLevel;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentJpaEntity;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerTest {

  private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final String OTHER_USER_ID = "22222222-2222-2222-2222-222222222222";
  private static final String ADMIN_USER_ID = "33333333-3333-3333-3333-333333333333";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityManager entityManager;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private ObjectStoragePort objectStoragePort;

  @BeforeEach
  void setUp() {
    ensureUserExists(UUID.fromString(USER_ID), "staff@platform.com", "Staff Member");
    ensureUserExists(UUID.fromString(OTHER_USER_ID), "other@platform.com", "Other Staff");
    ensureUserExists(UUID.fromString(ADMIN_USER_ID), "admin@platform.com", "Admin User");

    // Default mock behavior: drain input stream to simulate complete upload
    doAnswer(invocation -> {
      InputStream is = invocation.getArgument(1);
      byte[] buf = new byte[1024];
      while (is.read(buf) != -1) {
        // drain
      }
      return null;
    }).when(objectStoragePort).upload(anyString(), any(InputStream.class), anyLong(), anyString());
  }

  private void ensureUserExists(UUID uid, String email, String name) {
    UserJpaEntity user = entityManager.find(UserJpaEntity.class, uid);
    if (user == null) {
      user = UserJpaEntity.builder()
          .id(uid)
          .email(email)
          .passwordHash("hashed")
          .fullName(name)
          .enabled(true)
          .isInternal(true)
          .createdAt(java.time.Instant.now())
          .updatedAt(java.time.Instant.now())
          .build();
      entityManager.persist(user);
      entityManager.flush();
    }
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents - Should return 201 Created and metadata on valid upload (AC-1)")
  void shouldUploadDocumentSuccessfully() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "annual_report.pdf",
        "application/pdf",
        "Sample PDF Content for Testing".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/documents")
            .file(file)
            .param("title", "Annual Report 2026")
            .param("description", "Financial report"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", startsWith("/api/v1/documents/")))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.title", is("Annual Report 2026")))
        .andExpect(jsonPath("$.originalFileName", is("annual_report.pdf")))
        .andExpect(jsonPath("$.contentType", is("application/pdf")))
        .andExpect(jsonPath("$.checksumSha256", notNullValue()))
        .andExpect(jsonPath("$.status", is("UPLOADED")))
        .andExpect(jsonPath("$.accessLevel", is("INTERNAL")));
  }

  @Test
  @DisplayName("POST /api/v1/documents - Should return 401/403 when unauthenticated")
  void shouldRejectUnauthenticatedRequest() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "report.pdf", "application/pdf", "Content".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/documents").file(file))
        .andExpect(status().isForbidden()); // Spring security without principal returns 403 on denied endpoint
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"read:documents"})
  @DisplayName("POST /api/v1/documents - Should return 403 Forbidden when missing write:documents authority")
  void shouldRejectUnauthorizedRole() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "report.pdf", "application/pdf", "Content".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/documents").file(file))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents - Should return 415 Unsupported Media Type for disallowed extensions (AC-2)")
  void shouldRejectUnsupportedFileType() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "malicious_script.exe", "application/octet-stream", "MZBinary".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/documents").file(file))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status", is(415)))
        .andExpect(jsonPath("$.message", startsWith("Unsupported file type")));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents - Should return 413 Payload Too Large when file > 50MB (AC-2)")
  void shouldRejectOversizedFile() throws Exception {
    MockMultipartFile oversizedFile = new MockMultipartFile(
        "file", "large.pdf", "application/pdf", new byte[10]
    ) {
      @Override
      public long getSize() {
        return 51L * 1024 * 1024; // 51MB
      }
    };

    mockMvc.perform(multipart("/api/v1/documents").file(oversizedFile))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.status", is(413)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents - Should return 502 Bad Gateway when S3 storage throws StorageException (AC-6)")
  void shouldReturn502WhenStorageFails() throws Exception {
    doThrow(new StorageException("S3 Connection refused"))
        .when(objectStoragePort).upload(anyString(), any(), anyLong(), anyString());

    MockMultipartFile file = new MockMultipartFile(
        "file", "report.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "DOCX content".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/documents").file(file))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status", is(502)))
        .andExpect(jsonPath("$.message", startsWith("Object storage error")));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents - Should return 400 Bad Request when file is empty")
  void shouldRejectEmptyFile() throws Exception {
    MockMultipartFile emptyFile = new MockMultipartFile(
        "file", "empty.pdf", "application/pdf", new byte[0]
    );

    mockMvc.perform(multipart("/api/v1/documents").file(emptyFile))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status", is(400)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents/{id}/versions - Should return 200 OK and version metadata on valid upload (UC-DOC-02)")
  void shouldUploadNewVersionSuccessfully() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    MockMultipartFile v2File = new MockMultipartFile(
        "file",
        "annual_report_v2.pdf",
        "application/pdf",
        "Updated Content for Version 2".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/documents/{id}/versions", docId)
            .file(v2File)
            .param("changeSummary", "Updated financial statements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.documentId", is(docId.toString())))
        .andExpect(jsonPath("$.versionNumber", is(2)))
        .andExpect(jsonPath("$.changeSummary", is("Updated financial statements")))
        .andExpect(jsonPath("$.storageKey", is("documents/" + docId + "/v2/annual_report_v2.pdf")))
        .andExpect(jsonPath("$.checksumSha256", notNullValue()))
        .andExpect(jsonPath("$.uploadedByUserId", is(USER_ID)))
        .andExpect(jsonPath("$.createdAt", notNullValue()));
  }

  @Test
  @WithMockUser(username = OTHER_USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents/{id}/versions - Should return 403 Forbidden when user is not owner and not admin")
  void shouldRejectVersionUploadWhenNotOwnerAndNotAdmin() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    MockMultipartFile v2File = new MockMultipartFile(
        "file",
        "annual_report_v2.pdf",
        "application/pdf",
        "Unauthorized Update".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/documents/{id}/versions", docId)
            .file(v2File)
            .param("changeSummary", "Unauthorized edit"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status", is(403)));
  }

  @Test
  @WithMockUser(username = ADMIN_USER_ID, authorities = {"write:documents", "ROLE_ADMIN"})
  @DisplayName("POST /api/v1/documents/{id}/versions - Should allow admin to upload revision for any document")
  void shouldAllowAdminToUploadVersionForOtherUserDocument() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    MockMultipartFile v2File = new MockMultipartFile(
        "file",
        "annual_report_v2.pdf",
        "application/pdf",
        "Admin Approved Revision".getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(multipart("/api/v1/documents/{id}/versions", docId)
            .file(v2File)
            .param("changeSummary", "Admin hotfix revision"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versionNumber", is(2)))
        .andExpect(jsonPath("$.uploadedByUserId", is(ADMIN_USER_ID)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents/{id}/versions - Should return 404 Not Found when document does not exist")
  void shouldReturn404WhenDocumentNotFound() throws Exception {
    UUID missingDocId = UUID.randomUUID();

    MockMultipartFile file = new MockMultipartFile(
        "file", "report.pdf", "application/pdf", "Content".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/documents/{id}/versions", missingDocId)
            .file(file))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents/{id}/versions - Should return 415 Unsupported Media Type for disallowed extensions")
  void shouldRejectInvalidExtensionOnVersionUpload() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    MockMultipartFile invalidFile = new MockMultipartFile(
        "file", "script.sh", "application/x-sh", "echo 'bad'".getBytes()
    );

    mockMvc.perform(multipart("/api/v1/documents/{id}/versions", docId)
            .file(invalidFile))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status", is(415)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("POST /api/v1/documents/{id}/versions - Should return 413 Payload Too Large when version file > 50MB")
  void shouldRejectOversizedVersionFile() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    MockMultipartFile oversizedFile = new MockMultipartFile(
        "file", "large.pdf", "application/pdf", new byte[10]
    ) {
      @Override
      public long getSize() {
        return 51L * 1024 * 1024;
      }
    };

    mockMvc.perform(multipart("/api/v1/documents/{id}/versions", docId)
            .file(oversizedFile))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.status", is(413)));
  }

  // ============================================================================
  // UC-DOC-03: Configure Document Access Control Matrix
  // ============================================================================

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("PUT /api/v1/documents/{id}/permissions - Document owner should configure permissions successfully")
  void shouldConfigurePermissionsByOwnerSuccessfully() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    UpdateDocumentPermissionsRequest request = UpdateDocumentPermissionsRequest.builder()
        .accessLevel(AccessLevel.CONFIDENTIAL)
        .userGrants(List.of(
            UserGrantDto.builder().userId(UUID.fromString(OTHER_USER_ID)).permissionLevel(PermissionLevel.EDIT).build()
        ))
        .departmentGrants(Collections.emptyList())
        .roleGrants(Collections.emptyList())
        .build();

    mockMvc.perform(put("/api/v1/documents/{id}/permissions", docId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId", is(docId.toString())))
        .andExpect(jsonPath("$.accessLevel", is("CONFIDENTIAL")))
        .andExpect(jsonPath("$.userGrants[0].userId", is(OTHER_USER_ID)))
        .andExpect(jsonPath("$.userGrants[0].permissionLevel", is("EDIT")));
  }

  @Test
  @WithMockUser(username = ADMIN_USER_ID, authorities = {"ROLE_ADMIN"})
  @DisplayName("PUT /api/v1/documents/{id}/permissions - Admin should configure permissions successfully")
  void shouldConfigurePermissionsByAdminSuccessfully() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    UpdateDocumentPermissionsRequest request = UpdateDocumentPermissionsRequest.builder()
        .accessLevel(AccessLevel.PUBLIC)
        .userGrants(Collections.emptyList())
        .departmentGrants(Collections.emptyList())
        .roleGrants(Collections.emptyList())
        .build();

    mockMvc.perform(put("/api/v1/documents/{id}/permissions", docId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId", is(docId.toString())))
        .andExpect(jsonPath("$.accessLevel", is("PUBLIC")));
  }

  @Test
  @WithMockUser(username = OTHER_USER_ID, authorities = {"write:documents"})
  @DisplayName("PUT /api/v1/documents/{id}/permissions - Non-owner without manage:permissions should receive 403 Forbidden")
  void shouldRejectNonOwnerFromConfiguringPermissions() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    UpdateDocumentPermissionsRequest request = UpdateDocumentPermissionsRequest.builder()
        .accessLevel(AccessLevel.CONFIDENTIAL)
        .userGrants(Collections.emptyList())
        .departmentGrants(Collections.emptyList())
        .roleGrants(Collections.emptyList())
        .build();

    mockMvc.perform(put("/api/v1/documents/{id}/permissions", docId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status", is(403)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("PUT /api/v1/documents/{id}/permissions - Non-existent document should return 404 Not Found")
  void shouldReturnNotFoundWhenConfiguringPermissionsForNonExistentDocument() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    UpdateDocumentPermissionsRequest request = UpdateDocumentPermissionsRequest.builder()
        .accessLevel(AccessLevel.PUBLIC)
        .userGrants(Collections.emptyList())
        .departmentGrants(Collections.emptyList())
        .roleGrants(Collections.emptyList())
        .build();

    mockMvc.perform(put("/api/v1/documents/{id}/permissions", nonExistentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("GET /api/v1/documents/{id}/permissions - Document owner should retrieve current permissions successfully")
  void shouldGetPermissionsSuccessfully() throws Exception {
    UUID docId = UUID.randomUUID();
    createAndPersistDocument(docId, UUID.fromString(USER_ID));

    mockMvc.perform(get("/api/v1/documents/{id}/permissions", docId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId", is(docId.toString())))
        .andExpect(jsonPath("$.accessLevel", is("INTERNAL")));
  }

  @Test
  @WithMockUser(username = USER_ID, authorities = {"write:documents"})
  @DisplayName("GET /api/v1/documents/{id}/permissions - Non-existent document should return 404 Not Found")
  void shouldReturnNotFoundWhenGettingPermissionsForNonExistentDocument() throws Exception {
    UUID nonExistentId = UUID.randomUUID();

    mockMvc.perform(get("/api/v1/documents/{id}/permissions", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status", is(404)));
  }

  private void createAndPersistDocument(UUID docId, UUID ownerId) {
    DocumentJpaEntity doc = DocumentJpaEntity.builder()
        .id(docId)
        .title("Annual Report")
        .originalFileName("annual_report.pdf")
        .contentType("application/pdf")
        .fileSizeBytes(1024L)
        .checksumSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        .storageKey("documents/" + docId + "/v1/annual_report.pdf")
        .currentVersion(1)
        .status(DocumentStatus.UPLOADED)
        .uploadedByUserId(ownerId)
        .accessLevel(AccessLevel.INTERNAL)
        .createdAt(java.time.Instant.now())
        .updatedAt(java.time.Instant.now())
        .build();
    entityManager.persist(doc);
    entityManager.flush();
  }
}
