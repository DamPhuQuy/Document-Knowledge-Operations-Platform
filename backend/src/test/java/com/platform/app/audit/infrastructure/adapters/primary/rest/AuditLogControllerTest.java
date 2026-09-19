package com.platform.app.audit.infrastructure.adapters.primary.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.application.dto.AuditLogResponseDto;
import com.platform.app.audit.application.ports.inbound.GetAuditLogsUseCase;
import com.platform.app.audit.domain.model.AuditStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAuditLogsUseCase getAuditLogsUseCase;

    @MockitoBean
    private com.platform.app.audit.application.ports.inbound.RecordAuditLogUseCase recordAuditLogUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName(
        "Should return 200 OK with audit logs when authenticated as ADMIN"
    )
    void shouldReturnAuditLogsWhenAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        AuditLogResponseDto dto = AuditLogResponseDto.builder()
            .id(id)
            .action("UPLOAD_DOC")
            .resourceType("DOCUMENT")
            .resourceId("doc-1")
            .status(AuditStatus.SUCCESS)
            .details(Map.of("fileName", "doc.pdf"))
            .createdAt(Instant.now())
            .build();

        Page<AuditLogResponseDto> page = new PageImpl<>(List.of(dto));
        when(getAuditLogsUseCase.getAuditLogs(any(), any())).thenReturn(page);

        mockMvc
            .perform(get("/api/v1/audit-logs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].action", is("UPLOAD_DOC")))
            .andExpect(jsonPath("$.content[0].resourceType", is("DOCUMENT")));
    }

    @Test
    @WithMockUser(roles = "LEGAL_AUDITOR")
    @DisplayName("Should return 200 OK when authenticated as LEGAL_AUDITOR")
    void shouldReturnAuditLogsWhenLegalAuditor() throws Exception {
        Page<AuditLogResponseDto> page = new PageImpl<>(List.of());
        when(getAuditLogsUseCase.getAuditLogs(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "read:audit_logs")
    @DisplayName(
        "Should return 200 OK when authenticated with authority read:audit_logs"
    )
    void shouldReturnAuditLogsWhenHasAuthority() throws Exception {
        Page<AuditLogResponseDto> page = new PageImpl<>(List.of());
        when(getAuditLogsUseCase.getAuditLogs(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audit-logs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "read:documents")
    @DisplayName(
        "Should return 403 Forbidden when authenticated user lacks audit authority"
    )
    void shouldReturn403WhenStaff() throws Exception {
        mockMvc
            .perform(get("/api/v1/audit-logs"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401/403 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc
            .perform(get("/api/v1/audit-logs"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should pass filter parameters and pagination to use case")
    void shouldPassFiltersToUseCase() throws Exception {
        UUID userId = UUID.randomUUID();
        Page<AuditLogResponseDto> page = new PageImpl<>(List.of());
        when(getAuditLogsUseCase.getAuditLogs(any(), any())).thenReturn(page);

        mockMvc
            .perform(
                get("/api/v1/audit-logs")
                    .param("userId", userId.toString())
                    .param("action", "LOGIN")
                    .param("resourceType", "USER")
                    .param("status", "SUCCESS")
                    .param("page", "1")
                    .param("size", "25")
            )
            .andExpect(status().isOk());

        ArgumentCaptor<AuditLogQueryFilter> filterCaptor =
            ArgumentCaptor.forClass(AuditLogQueryFilter.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
            Pageable.class
        );

        verify(getAuditLogsUseCase).getAuditLogs(
            filterCaptor.capture(),
            pageableCaptor.capture()
        );

        AuditLogQueryFilter filter = filterCaptor.getValue();
        assertThat(filter.getUserId()).isEqualTo(userId);
        assertThat(filter.getAction()).isEqualTo("LOGIN");
        assertThat(filter.getResourceType()).isEqualTo("USER");
        assertThat(filter.getStatus()).isEqualTo(AuditStatus.SUCCESS);

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
    }
}
