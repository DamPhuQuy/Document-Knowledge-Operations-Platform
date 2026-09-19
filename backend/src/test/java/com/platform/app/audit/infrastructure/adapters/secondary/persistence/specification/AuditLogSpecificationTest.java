package com.platform.app.audit.infrastructure.adapters.secondary.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.domain.model.AuditStatus;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity.AuditLogJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class AuditLogSpecificationTest {

    @Test
    @DisplayName("Should return conjunction predicate when filter is null")
    void shouldReturnConjunctionWhenFilterIsNull() {
        Specification<AuditLogJpaEntity> spec = AuditLogSpecification.withFilter(null);

        Root<AuditLogJpaEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(conjunction);
        verify(cb).conjunction();
    }

    @Test
    @DisplayName("Should build predicates for all non-empty filter fields")
    void shouldBuildPredicatesForFilterFields() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        AuditLogQueryFilter filter = AuditLogQueryFilter.builder()
            .userId(userId)
            .action("UPLOAD_DOC")
            .resourceType("DOCUMENT")
            .resourceId("doc-123")
            .status(AuditStatus.SUCCESS)
            .startDate(now.minusSeconds(3600))
            .endDate(now)
            .build();

        Specification<AuditLogJpaEntity> spec = AuditLogSpecification.withFilter(filter);

        Root<AuditLogJpaEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path userIdPath = mock(Path.class);
        Path actionPath = mock(Path.class);
        Path resourceTypePath = mock(Path.class);
        Path resourceIdPath = mock(Path.class);
        Path statusPath = mock(Path.class);
        Path createdAtPath = mock(Path.class);

        when(root.get("userId")).thenReturn(userIdPath);
        when(root.get("action")).thenReturn(actionPath);
        when(root.get("resourceType")).thenReturn(resourceTypePath);
        when(root.get("resourceId")).thenReturn(resourceIdPath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("createdAt")).thenReturn(createdAtPath);

        Expression<String> upperAction = mock(Expression.class);
        Expression<String> upperResourceType = mock(Expression.class);
        when(cb.upper(actionPath)).thenReturn(upperAction);
        when(cb.upper(resourceTypePath)).thenReturn(upperResourceType);

        Predicate p = mock(Predicate.class);
        when(cb.equal(any(), any())).thenReturn(p);
        when(cb.greaterThanOrEqualTo(any(), any(Instant.class))).thenReturn(p);
        when(cb.lessThanOrEqualTo(any(), any(Instant.class))).thenReturn(p);

        Predicate andPredicate = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(andPredicate);
        verify(cb).and(any(Predicate[].class));
    }
}
