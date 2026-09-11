# Research: CHG-IAM-03 Refactor JWT Configuration to ConfigurationProperties

<research_context task_id="CHG-IAM-03" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. -->
<research_status>
  <phase>RESEARCH</phase>
  <mode>READ-ONLY</mode>
  <research_owner>@engineer</research_owner>
  <last_updated>2026-09-11</last_updated>
</research_status>

---

## 1. Current Behavior & Baseline Findings

<current_behavior>
  `JwtTokenProviderAdapter` injected JWT properties using `@Value("${app.jwt.secret}")` and `@Value("${app.jwt.expiration-ms}")`.
  
  ### Confirmed Findings:
  1. `build.gradle` already includes `spring-boot-configuration-processor` (both `compileOnly` and `annotationProcessor`) and `spring-boot-starter-validation`.
  2. `application.yaml` defines `app.jwt` with `secret`, `expiration-ms`, and `refresh-expiration-ms`.
  3. `SecurityConfig` resides in `com.platform.app.iam.infrastructure.adapters.secondary.security.config`, making it the natural location for `JwtProperties`.
</current_behavior>

---

## 2. Invariants & Guardrails

<invariants>
  1. Immutable Java record for properties holder.
  2. Strict $\ge$ 256-bit (32 bytes) validation for HMAC-SHA256 secret.
  3. Full backward compatibility with existing tests.
</invariants>

---

## 3. Exit Criteria

<exit_criteria>
  - [x] Baseline documented.
  - [x] Dependency availability confirmed.
  - [x] Ready for implementation.
</exit_criteria>

</research_context>
