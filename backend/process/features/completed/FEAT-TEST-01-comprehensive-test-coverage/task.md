# Task: FEAT-TEST-01 Comprehensive Unit and Slice Testing Suite

<task_spec version="3.0" framework="RIPER-5">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL (master state record)
     ════════════════════════════════════════════ -->
<task_control>
  <status>COMPLETED</status>
  <spec_level>S2</spec_level>
  <priority>P1</priority>
  <risk>LOW</risk>
  <estimated_story_points>5</estimated_story_points>
  <working_mode>DELEGATED</working_mode>
  <current_phase>REVIEW</current_phase>
  <owner>@engineer</owner>
  <decision_owner>@engineer</decision_owner>
  <created>2026-09-19</created>
  <last_updated>2026-09-19</last_updated>
</task_control>

---

## 1. Specification (Pillar 1: Task / Spec)

<specification>
  <goal>
    Thiết lập và triển khai bộ kiểm thử toàn diện (comprehensive test suite) bao phủ 1:1 các thành phần trong `src/main/java` sang `src/test/java`, bao gồm Domain Unit Tests, Application Use Case Tests (Mockito), Slice Tests (@WebMvcTest, @DataJpaTest), Security Filter Tests, và Utility Tests theo kiến trúc Clean/Hexagonal Architecture.
  </goal>

  <current_behavior>
    - Toàn bộ backend có 175 file Java trong `src/main/java` nhưng chỉ có 38 file kiểm thử trong `src/test/java`.
    - Nhiều thành phần lõi chưa có test trực tiếp:
      * `RegisterService.java` (Use Case đăng ký & OTP).
      * `RegistrationOtpEventListener.java` (Event listener gửi OTP email).
      * `DocumentMetadataService.java` (Xử lý metadata upload và versioning).
      * `RestExceptionHandler.java` (Global exception handler cho HTTP error response).
      * `JwtAuthenticationFilter.java` (Filter xác thực JWT trong Security Chain).
      * `RedisOtpAdapter.java`, `LoggingEmailNotificationAdapter.java`.
      * Các Domain Models (`Role`, `Permission`, `DocumentDepartmentAccess`, `DocumentRoleAccess`, `DocumentUserAccess`).
      * Các Secondary Persistence Adapters (`RefreshTokenRepositoryAdapter`, `RoleRepositoryAdapter`, `UserRepositoryAdapter`, `DocumentVersionRepositoryAdapter`) mới chỉ được test gộp hoặc thiếu test mapping/edge cases.
  </current_behavior>

  <expected_behavior>
    - Mỗi lớp chức năng (class, service, adapter, controller, model logic) trong `src/main/java` đều có lớp kiểm thử tương ứng tương đương 1:1 trong `src/test/java`.
    - Tất cả unit tests chạy độc lập, tốc độ mili-giây, không phụ thuộc ngoại cảnh (isolated).
    - Các slice tests (@WebMvcTest, @DataJpaTest) kiểm chứng tính đúng đắn của HTTP contracts và Database persistence mappings.
    - `./gradlew test` và `./gradlew check` chạy thành công 100%, không flaky, không warning nghiêm trọng.
  </expected_behavior>

  <actor_authorization>
    Hệ thống phát triển / CI-CD pipeline / Developer.
  </actor_authorization>

  <invariants>
    - Không làm thay đổi logic nghiệp vụ trong `src/main/java` (trừ khi phát hiện bug nghiêm trọng cần thảo luận qua Gate G1/G2).
    - Bộ test phải tuân thủ chuẩn Clean Architecture: Domain Tests không load Spring Context; Application Service Tests chỉ mock Outbound Ports; Slice tests chỉ load slice context tối thiểu.
    - Mã kiểm thử tuân thủ spotless format và không chứa logic flaky / timing-dependent.
  </invariants>

  <out_of_scope>
    - Không viết test thừa thãi cho các pure interfaces (Ports, Spring Data Interfaces) không có default method.
    - Không thêm dependency bên ngoài làm tăng kích thước artifact ngoài các test starter đã có trong `build.gradle`.
    - Không can thiệp sửa đổi Liquibase scripts hay cơ sở dữ liệu production.
  </out_of_scope>

  <acceptance_criteria>
    - [x] AC-1: Hoàn thành kiểm thử cho toàn bộ Application Services còn thiếu (`RegisterService`, `DocumentMetadataService`) và Listeners (`RegistrationOtpEventListener`).
    - [x] AC-2: Hoàn thành kiểm thử cho Security & Infrastructure Filters (`JwtAuthenticationFilter`, `SecurityConfig`, `RestExceptionHandler`).
    - [x] AC-3: Hoàn thành kiểm thử cho các Secondary Adapters (`RedisOtpAdapter`, `LoggingEmailNotificationAdapter`, `DocumentVersionRepositoryAdapter`, `PersistenceAdapters`).
    - [x] AC-4: Hoàn thành kiểm thử cho Domain Models và Value Objects (`Role`, `Permission`, `DocumentUserAccess`, `DocumentDepartmentAccess`, `DocumentRoleAccess`, `AuditStatus`, `DocumentStatus`).
    - [x] AC-5: Hoàn thành kiểm thử cho DTO validation và Request/Response mapping contracts.
    - [x] AC-6: Toàn bộ `./gradlew test` pass 100% với thời gian thực thi tối ưu.
  </acceptance_criteria>

  <definition_of_ready>
    - [x] Target outcome and acceptance criteria are verifiable without guessing.
    - [x] Invariants and <out_of_scope> boundaries are explicit.
    - [x] Open questions resolved or scheduled in decision.md (No speculative coding).
  </definition_of_ready>
</specification>

---

## 2. Context Boundaries (Pillar 2: Context)

<context_boundaries>
  <target_files>
    - `src/test/java/com/platform/app/**` — [Các file kiểm thử đơn vị, slice, adapter]
  </target_files>

  <context_groups>
    - tests
    - clean-architecture
  </context_groups>

  <source_of_truth>
    <requirement>Yêu cầu người dùng: Cung cấp kiến thức viết test & xây dựng bộ test toàn diện 1:1 cho toàn bộ src</requirement>
    <architecture>Clean Architecture & Hexagonal Architecture (Domain -> Application -> Infrastructure)</architecture>
    <existing_behavior>src/test/java hiện hữu (38 test classes)</existing_behavior>
    <tests>./gradlew test</tests>
  </source_of_truth>
</context_boundaries>

---

## 3. Verification Strategy

<verification_strategy>

| AC / Risk | Evidence required | Verifier |
|---|---|---|
| AC-1: Services & Listeners | Tất cả use cases và listeners có unit test với 100% path coverage | `./gradlew test --tests "*ServiceTest" --tests "*ListenerTest"` |
| AC-2: Security & Filters | Filter chains, JWT parsing, exception handling được kiểm thử | `./gradlew test --tests "*FilterTest" --tests "*SecurityTest" --tests "*ExceptionHandlerTest"` |
| AC-3: Adapters | Các persistence, redis, email, storage adapter được test | `./gradlew test --tests "*AdapterTest"` |
| AC-4: Domain Models | Domain invariants, builders, mutations được test | `./gradlew test --tests "*ModelTest" --tests "*Test"` |
| AC-5 & AC-6: Toàn bộ suite | Toàn bộ suite test chạy thành công | `./gradlew test && ./gradlew check` |

</verification_strategy>

---

## 4. Decisions

<decisions>
  <approved_decisions>
    - D1: Sử dụng thuần JUnit 5, AssertJ (`assertThat`), Mockito (`@Mock`, `@InjectMocks`) cho Domain & Application Unit Tests để đạt tốc độ thực thi cao nhất (zero context bootstrap).
    - D2: Sử dụng `@WebMvcTest` + MockMvc cho Controller Slices và `@DataJpaTest` cho Persistence Slices khi cần kiểm tra SQL/mapping.
  </approved_decisions>

  <open_decisions>
    - OD-1: Phân bổ thứ tự triển khai các test slice theo độ ưu tiên trong INNOVATE/PLAN phase.
  </open_decisions>
</decisions>

---

## 5. RIPER-5 Execution Plan (Pillar 4: Loop)

<execution_plan>
  <phase name="Research" order="1">
    - [x] Ingest task spec, domain invariants, and out-of-scope boundaries.
    - [x] Rà soát cấu trúc 175 files trong `src/main/java` và 38 files trong `src/test/java`.
    - [x] Phân loại kiến trúc kiểm thử và lập ma trận ánh xạ 1:1.
    - [x] Tạo tài liệu `research.md`.
    <gate id="G0" label="Research Complete">
      - [x] Current behavior understood and documented.
      - [x] Execution flow traced and test gaps identified.
      - [x] No unresolved research blocker.
    </gate>
  </phase>

  <phase name="Innovate" order="2">
    - [ ] Lựa chọn giải pháp tổ chức bộ test: Unit Test độc lập vs Integration Slice vs Contract Test.
    - [ ] Tạo `decision.md`.
    <gate id="G1" label="Gate 1 — Decision Approved">
      - [ ] Options reviewed and trade-offs analyzed.
      - [ ] Selected option recorded in `decision.md`.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>

  <phase name="Plan" order="3">
    - [ ] Chia nhỏ các slice thực thi theo từng module (`iam`, `document`, `audit`, `shared`).
    - [ ] Xác định scope contract và rollback point cho từng slice trong `plan.md`.
    <gate id="G2" label="Gate 2 — Plan Approved">
      - [ ] Every slice has a verifier.
      - [ ] Allowed/forbidden file scope is defined.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>

  <phase name="Execute" order="4">
    - [ ] Triển khai từng slice test.
    - [ ] Chạy `./gradlew test` sau mỗi slice.
    - [ ] Cập nhật `state.md`.
  </phase>

  <phase name="Review" order="5">
    - [ ] Review toàn bộ diff và độ phủ test.
    - [ ] Tạo `review.md`.
    <gate id="G3" label="Gate 3 — Review Passed">
      - [ ] All AC verified with evidence.
      - [ ] Review decision: PASS.
      <approved_by></approved_by>
      <approved_date></approved_date>
    </gate>
  </phase>
</execution_plan>

---

## 6. Guardrails & Escalation (Pillar 3 & 4: Harness)

<guardrails>
  <stop_conditions>
    - Phát hiện logic code trong `src/main` có bug cần refactor lớn ngoài phạm vi test.
    - Thất bại liên tiếp quá retry budget mà không rõ nguyên nhân.
  </stop_conditions>

  <retry_budget max_attempts="3">
    Maximum 3 consecutive attempts per distinct failure symptom before halting.
  </retry_budget>
</guardrails>

</task_spec>
