package com.platform.app.audit.infrastructure.adapters.secondary.persistence.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity.AuditLogJpaEntity;

import jakarta.persistence.criteria.Predicate;

public final class AuditLogSpecification {

  private AuditLogSpecification() {}

  public static Specification<AuditLogJpaEntity> withFilter(AuditLogQueryFilter filter) {
    return (root, query, cb) -> {
      if (filter == null) {
        return cb.conjunction();
      }

      List<Predicate> predicates = new ArrayList<>();

      if (filter.getUserId() != null) {
        predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
      }

      if (filter.getAction() != null && !filter.getAction().isBlank()) {
        predicates.add(
            cb.equal(cb.upper(root.get("action")), filter.getAction().trim().toUpperCase()));
      }

      if (filter.getResourceType() != null && !filter.getResourceType().isBlank()) {
        predicates.add(
            cb.equal(
                cb.upper(root.get("resourceType")),
                filter.getResourceType().trim().toUpperCase()));
      }

      if (filter.getResourceId() != null && !filter.getResourceId().isBlank()) {
        predicates.add(cb.equal(root.get("resourceId"), filter.getResourceId().trim()));
      }

      if (filter.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), filter.getStatus()));
      }

      if (filter.getStartDate() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
      }

      if (filter.getEndDate() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
