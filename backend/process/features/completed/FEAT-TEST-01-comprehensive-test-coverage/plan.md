# Plan: PLAN-TEST-01 Comprehensive Unit and Slice Testing Suite

<execution_plan task_id="FEAT-TEST-01" plan_id="PLAN-TEST-01" version="2.0" framework="RIPER-5">

<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-19</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[FEAT-TEST-01 task.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/FEAT-TEST-01-comprehensive-test-coverage/task.md)</task_spec>
  <research>[FEAT-TEST-01 research.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/FEAT-TEST-01-comprehensive-test-coverage/research.md)</research>
  <decision>[DEC-TEST-01 decision.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/FEAT-TEST-01-comprehensive-test-coverage/decision.md) (Option A: Clean Layered & Slice 1:1)</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/test/java/com/platform/app/iam/**` — [IAM unit, slice, filter, and adapter tests]
    - `src/test/java/com/platform/app/document/**` — [Document services, models, and adapter tests]
    - `src/test/java/com/platform/app/audit/**` — [Audit domain, specification, and adapter tests]
    - `src/test/java/com/platform/app/shared/**` — [Shared constants and utils tests]
    - `process/features/active/FEAT-TEST-01-comprehensive-test-coverage/**` — [RIPER-5 tracking artifacts]
  </allowed_files>
  <forbidden_files>
    - `src/main/resources/db/changelog/**` — [DB migration schemas are locked]
    - `build.gradle` — [Build dependencies are stable]
  </forbidden_files>
  <allowed_commands>
    - `./gradlew test`
    - `./gradlew check`
    - `./gradlew spotlessApply`
  </allowed_commands>
  <restricted_operations>
    - Không sửa đổi logic trong `src/main/java` trừ khi phát hiện bug nghiêm trọng cần báo cáo.
  </restricted_operations>
  <required_approvals>
    - [AUTO: DELEGATED] Tự động thực thi liên tục qua các vertical slices.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Missing IAM Application Services & Listeners | IAM Application | `RegisterServiceTest.java`, `RegistrationOtpEventListenerTest.java` | AC-1 | `./gradlew test --tests "*RegisterServiceTest" --tests "*RegistrationOtpEventListenerTest"` | LOW | AUTO | Git checkout |
| S2 | Missing Document Application Services & Models | Document Module | `DocumentMetadataServiceTest.java`, `DocumentStatusTest.java`, `DocumentUserAccessTest.java`, `DocumentDepartmentAccessTest.java`, `DocumentRoleAccessTest.java` | AC-1, AC-4 | `./gradlew test --tests "*DocumentMetadataServiceTest" --tests "*Document*AccessTest"` | LOW | AUTO | Git checkout |
| S3 | Missing IAM Domain Models & Security Filter & Handler | IAM Security & Models | `RoleTest.java`, `PermissionTest.java`, `JwtAuthenticationFilterTest.java`, `RestExceptionHandlerTest.java` | AC-2, AC-4 | `./gradlew test --tests "*RoleTest" --tests "*PermissionTest" --tests "*JwtAuthenticationFilterTest" --tests "*RestExceptionHandlerTest"` | LOW | AUTO | Git checkout |
| S4 | Secondary Adapters & Specifications | Adapters & Shared | `RedisOtpAdapterTest.java`, `LoggingEmailNotificationAdapterTest.java`, `DocumentVersionRepositoryAdapterTest.java`, `AuditLogSpecificationTest.java`, `AuditStatusTest.java`, `LoggingConstantsTest.java` | AC-3, AC-4 | `./gradlew test` | LOW | AUTO | Git checkout |
| S5 | Comprehensive Suite Verification & Linter | Full Test Suite | All tests | AC-5, AC-6 | `./gradlew test && ./gradlew check` | LOW | AUTO | Git clean/stash |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Kiểm thử toàn diện cho RegisterService và RegistrationOtpEventListener</objective>
    <change>Tạo các file kiểm thử đơn vị với Mockito</change>
    <allowed_files>
      - `src/test/java/com/platform/app/iam/application/services/RegisterServiceTest.java`
      - `src/test/java/com/platform/app/iam/application/listener/RegistrationOtpEventListenerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: RegisterServiceTest bao phủ happy path, email trùng, password encoding, OTP 6 chữ số, Redis TTL, và emit event.
      - [ ] AC-1: RegistrationOtpEventListenerTest bao phủ dispatch email notification khi nhận UserRegisteredOtpEvent.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.iam.application.services.RegisterServiceTest" --tests "com.platform.app.iam.application.listener.RegistrationOtpEventListenerTest"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all tests passing</expected_evidence>
    <rollback_point>git checkout HEAD -- src/test/java/com/platform/app/iam/application/</rollback_point>
    <stop_conditions>Lỗi compilation hoặc classpath conflict</stop_conditions>
  </slice>

  <slice id="S2">
    <objective>Kiểm thử DocumentMetadataService và các Domain Models của Document</objective>
    <change>Tạo DocumentMetadataServiceTest và các unit tests cho models & enum</change>
    <allowed_files>
      - `src/test/java/com/platform/app/document/application/services/DocumentMetadataServiceTest.java`
      - `src/test/java/com/platform/app/document/domain/model/DocumentStatusTest.java`
      - `src/test/java/com/platform/app/document/domain/model/DocumentUserAccessTest.java`
      - `src/test/java/com/platform/app/document/domain/model/DocumentDepartmentAccessTest.java`
      - `src/test/java/com/platform/app/document/domain/model/DocumentRoleAccessTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: DocumentMetadataServiceTest kiểm tra persistMetadata và persistVersionMetadata.
      - [ ] AC-4: Các Domain Models và Enums kiểm tra builder, getters, equals, hashCode, invariants.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "com.platform.app.document.application.services.DocumentMetadataServiceTest" --tests "com.platform.app.document.domain.model.*Test"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all tests passing</expected_evidence>
    <rollback_point>git checkout HEAD -- src/test/java/com/platform/app/document/</rollback_point>
    <stop_conditions>Lỗi compilation</stop_conditions>
  </slice>

  <slice id="S3">
    <objective>Kiểm thử IAM Domain Models, JwtAuthenticationFilter, và RestExceptionHandler</objective>
    <change>Tạo RoleTest, PermissionTest, JwtAuthenticationFilterTest, RestExceptionHandlerTest</change>
    <allowed_files>
      - `src/test/java/com/platform/app/iam/domain/model/RoleTest.java`
      - `src/test/java/com/platform/app/iam/domain/model/PermissionTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/security/filter/JwtAuthenticationFilterTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandlerTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-2: JwtAuthenticationFilterTest kiểm tra parsing Bearer token hợp lệ, thiếu token, token hết hạn/lỗi.
      - [ ] AC-2: RestExceptionHandlerTest kiểm tra bắt mọi domain exception và chuyển đổi thành ErrorResponse với mã HTTP tương ứng.
      - [ ] AC-4: RoleTest và PermissionTest kiểm tra thuộc tính và permission immutability.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test --tests "*RoleTest" --tests "*PermissionTest" --tests "*JwtAuthenticationFilterTest" --tests "*RestExceptionHandlerTest"
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all tests passing</expected_evidence>
    <rollback_point>git checkout HEAD -- src/test/java/com/platform/app/iam/</rollback_point>
    <stop_conditions>Lỗi compilation</stop_conditions>
  </slice>

  <slice id="S4">
    <objective>Kiểm thử Secondary Adapters, AuditLogSpecification, và Shared</objective>
    <change>Tạo RedisOtpAdapterTest, LoggingEmailNotificationAdapterTest, DocumentVersionRepositoryAdapterTest, AuditLogSpecificationTest, AuditStatusTest, LoggingConstantsTest</change>
    <allowed_files>
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/redis/RedisOtpAdapterTest.java`
      - `src/test/java/com/platform/app/iam/infrastructure/adapters/secondary/notification/LoggingEmailNotificationAdapterTest.java`
      - `src/test/java/com/platform/app/document/infrastructure/adapters/secondary/persistence/adapter/DocumentVersionRepositoryAdapterTest.java`
      - `src/test/java/com/platform/app/audit/infrastructure/adapters/secondary/persistence/specification/AuditLogSpecificationTest.java`
      - `src/test/java/com/platform/app/audit/domain/model/AuditStatusTest.java`
      - `src/test/java/com/platform/app/shared/infrastructure/logging/LoggingConstantsTest.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-3: RedisOtpAdapterTest kiểm tra save, get, delete và TTL qua RedisTemplate mock.
      - [ ] AC-3: DocumentVersionRepositoryAdapterTest kiểm tra mapping Entity <-> Domain và findByDocumentId.
      - [ ] AC-3: AuditLogSpecificationTest kiểm tra predicates lọc audit log.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with all tests passing</expected_evidence>
    <rollback_point>git checkout HEAD -- src/test/java/</rollback_point>
    <stop_conditions>Lỗi compilation</stop_conditions>
  </slice>

  <slice id="S5">
    <objective>Chạy toàn bộ test suite và code quality check</objective>
    <change>Đảm bảo toàn bộ 50+ test files pass 100% và spotless linter sạch</change>
    <allowed_files>
      - `src/test/java/**`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-5, AC-6: `./gradlew test && ./gradlew check` chạy pass hoàn toàn.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew test && ./gradlew check
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL in ~30s with zero failures</expected_evidence>
    <rollback_point>none</rollback_point>
    <stop_conditions>Bất kỳ lỗi test failure nào</stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - `src/test/java/com/platform/app/**`
    - `process/features/active/FEAT-TEST-01-comprehensive-test-coverage/**`
  </allowed>
  <forbidden>
    - `src/main/resources/db/changelog/**`
    - `build.gradle`
  </forbidden>
</scope_contract>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a verifier.
  - [x] Allowed/forbidden file scope is defined.
  - [x] Rollback point defined per slice.
  - [x] Stop conditions defined.
  - [x] Plan approved.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-19</approved_date>
</gate>

</execution_plan>
