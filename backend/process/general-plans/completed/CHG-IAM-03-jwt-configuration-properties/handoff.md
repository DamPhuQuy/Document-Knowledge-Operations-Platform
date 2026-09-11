# Handoff: CHG-IAM-03 Refactor JWT Configuration to ConfigurationProperties

<handoff task_id="CHG-IAM-03" version="2.0" framework="RIPER-5">

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>[`process/general-plans/completed/CHG-IAM-03-jwt-configuration-properties/review.md`](review.md)</review_artifact>
  <completed_date>2026-09-11</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Replaced loose `@Value` properties in `JwtTokenProviderAdapter` with an immutable, type-safe Java Record `JwtProperties` using Spring Boot `@ConfigurationProperties(prefix = "app.jwt")` and Jakarta bean validation (`@NotBlank`, `@Size(min = 32)`, `@Positive`). Added `@ConfigurationPropertiesScan` to `AppApplication` and updated tests.
</what_changed>

<main_changes>
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/JwtProperties.java`: Java record holding validated `secret`, `expirationMs`, and `refreshExpirationMs`.
  - `src/main/java/com/platform/app/AppApplication.java`: Added `@ConfigurationPropertiesScan`.
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/adapter/JwtTokenProviderAdapter.java`: Injected `JwtProperties` directly into constructor.
  - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/JwtTokenProviderAdapterTest.java`: Updated test instances.
</main_changes>

---

## 2. What Proves It

<evidence>
  `./gradlew test` and `./gradlew check` passed 100% cleanly.
</evidence>

</handoff>
