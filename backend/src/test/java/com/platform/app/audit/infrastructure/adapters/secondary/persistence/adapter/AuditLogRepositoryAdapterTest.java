package com.platform.app.audit.infrastructure.adapters.secondary.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.domain.model.AuditLog;
import com.platform.app.audit.domain.model.AuditStatus;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity.AuditLogJpaEntity;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.repository.SpringDataAuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryAdapterTest {

    @Mock
    private SpringDataAuditLogRepository repository;

    private AuditLogRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AuditLogRepositoryAdapter(repository);
    }

    @Test
    @DisplayName("Should save audit log generating UUID if id is null")
    void shouldSaveAuditLog() {
        UUID userId = UUID.randomUUID();
        AuditLog domain = AuditLog.builder()
            .userId(userId)
            .action("LOGIN")
            .resourceType("USER")
            .resourceId(userId.toString())
            .ipAddress("127.0.0.1")
            .userAgent("Mozilla/5.0")
            .status(AuditStatus.SUCCESS)
            .details(Map.of("browser", "Chrome"))
            .createdAt(Instant.now())
            .build();

        when(repository.save(any(AuditLogJpaEntity.class))).thenAnswer(
            invocation -> {
                AuditLogJpaEntity entity = invocation.getArgument(0);
                return entity;
            }
        );

        AuditLog saved = adapter.save(domain);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAction()).isEqualTo("LOGIN");
        assertThat(saved.getDetails()).containsEntry("browser", "Chrome");

        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(
            AuditLogJpaEntity.class
        );
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("LOGIN");
    }

    @Test
    @DisplayName("Should find audit log by ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
            .id(id)
            .action("UPLOAD_DOC")
            .resourceType("DOCUMENT")
            .status(AuditStatus.SUCCESS)
            .createdAt(Instant.now())
            .build();

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        Optional<AuditLog> found = adapter.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
        assertThat(found.get().getAction()).isEqualTo("UPLOAD_DOC");
    }

    @Test
    @DisplayName("Should query audit logs with filter and pageable")
    void shouldFindAllWithFilter() {
        UUID id = UUID.randomUUID();
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
            .id(id)
            .action("DELETE_DOC")
            .resourceType("DOCUMENT")
            .status(AuditStatus.SUCCESS)
            .createdAt(Instant.now())
            .build();

        Page<AuditLogJpaEntity> entityPage = new PageImpl<>(List.of(entity));
        when(
            repository.findAll(any(Specification.class), any(Pageable.class))
        ).thenReturn(entityPage);

        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
            .action("DELETE_DOC")
            .resourceType("DOCUMENT")
            .build();
        Pageable pageable = PageRequest.of(0, 10);

        Page<AuditLog> result = adapter.findAll(filter, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo(
            "DELETE_DOC"
        );
    }
}
