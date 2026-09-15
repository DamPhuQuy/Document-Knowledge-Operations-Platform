package com.platform.app.document.infrastructure.adapters.primary.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.document.application.ports.outbound.ObjectStoragePort;
import com.platform.app.document.domain.exception.StorageException;
import com.platform.app.iam.infrastructure.adapters.secondary.persistence.entity.UserJpaEntity;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerTest {

  private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityManager entityManager;

  @MockitoBean
  private ObjectStoragePort objectStoragePort;

  @BeforeEach
  void setUp() {
    // Ensure test user exists in DB to satisfy fk_documents_uploaded_by
    UUID uid = UUID.fromString(USER_ID);
    UserJpaEntity user = entityManager.find(UserJpaEntity.class, uid);
    if (user == null) {
      user = UserJpaEntity.builder()
          .id(uid)
          .email("staff@platform.com")
          .passwordHash("hashed")
          .fullName("Staff Member")
          .enabled(true)
          .isInternal(true)
          .createdAt(java.time.Instant.now())
          .updatedAt(java.time.Instant.now())
          .build();
      entityManager.persist(user);
      entityManager.flush();
    }

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
        .andExpect(jsonPath("$.fileType", is("PDF")))
        .andExpect(jsonPath("$.checksumSha256", notNullValue()))
        .andExpect(jsonPath("$.currentVersion", is(1)))
        .andExpect(jsonPath("$.processingStatus", is("UPLOADED")))
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
}
