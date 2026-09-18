# Task Lite: AUTH-004 Register OTP Verification with Redis & Asynchronous Email

<task_lite version="3.0" framework="RIPER-5-Lite">

<!-- ════════════════════════════════════════════
     SECTION 0 — TASK CONTROL
     ════════════════════════════════════════════ -->
<task_control>
  <track>LITE</track>
  <status>COMPLETED</status>
  <priority>P1</priority>
  <working_mode>PAIR</working_mode>
  <current_phase>REVIEW</current_phase>
  <owner>@engineer</owner>
</task_control>

---

## 1. Intent & Specification

<specification>
  <goal>
    Triển khai quy trình xác thực OTP khi người dùng đăng ký tài khoản (User Registration Flow):
    1. Khi hoàn tất POST /api/v1/auth/register, người dùng được tạo với trạng thái chưa kích hoạt (enabled = false).
    2. Sinh mã OTP ngẫu nhiên (6 chữ số) và lưu trữ trong Redis với thời gian sống ngắn hạn (TTL = 5 phút).
    3. Trigger sự kiện bất đồng bộ (@Async Event) để gửi email chứa mã OTP đến người dùng.
    4. Cung cấp endpoint POST /api/v1/auth/verify-otp để người dùng nhập mã OTP xác thực; khi hợp lệ sẽ kích hoạt tài khoản (enabled = true) và dọn dẹp OTP trong Redis.
  </goal>

  <invariants>
    - Tài khoản chưa xác thực OTP (enabled = false) tuyệt đối không được phép đăng nhập (AccountDisabledException).
    - Mã OTP trong Redis phải có TTL ngắn hạn (5 phút) và bị thu hồi/xóa ngay sau khi xác thực thành công.
    - Gửi email OTP phải diễn ra bất đồng bộ (@Async), không làm chậm hoặc nghẽn response của request đăng ký.
    - Tuân thủ nghiêm ngặt Clean Architecture / Hexagonal Architecture: Domain/Application không phụ thuộc trực tiếp vào Redis hay Mail client mà thông qua Outbound Ports.
    - Không làm gián đoạn các test suite hiện tại; cung cấp in-memory fallback hoặc test mock cho Redis & Email.
  </invariants>

  <acceptance_criteria>
    - [x] AC-1: Bổ sung dependencies (Spring Data Redis, Redis service trong docker-compose.yaml và cấu hình application.yaml).
    - [x] AC-2: Định nghĩa Outbound Ports `OtpRepositoryPort` và `EmailNotificationPort`.
    - [x] AC-3: Cài đặt `RedisOtpAdapter` sử dụng StringRedisTemplate (kèm TTL 5 phút và in-memory fallback an toàn) và `LoggingEmailNotificationAdapter`.
    - [x] AC-4: Cập nhật `RegisterService` tạo User với `enabled = false`, sinh OTP, lưu vào Redis, và publish `UserRegisteredOtpEvent`.
    - [x] AC-5: Cài đặt Asynchronous Event Listener (`RegistrationOtpEventListener`) tiếp nhận event và gọi `EmailNotificationPort.sendOtpEmail`.
    - [x] AC-6: Định nghĩa `VerifyOtpUseCase`, `VerifyOtpCommand`, `VerifyOtpService` và `User.enable()`.
    - [x] AC-7: Thêm DTOs `VerifyOtpRequest`, `VerifyOtpResponse` và endpoint `POST /api/v1/auth/verify-otp` trong `AuthController`.
    - [x] AC-8: Xử lý ngoại lệ `InvalidOtpException` và `OtpExpiredException` (400 Bad Request) trong `RestExceptionHandler`.
    - [x] AC-9: Viết unit test và integration test hoàn chỉnh cho toàn bộ luồng đăng ký + xác thực OTP (`VerifyOtpServiceTest`, `AuthControllerTest`).
  </acceptance_criteria>

  <definition_of_ready>
    - [x] Intent and acceptance criteria are clear without assumptions.
    - [x] Allowed files in Section 2 are identified.
  </definition_of_ready>
</specification>

---

## 2. Scope Contract & File Whitelist

<scope_contract>
  <allowed_files>
    <file>backend/build.gradle</file>
    <file>backend/src/main/resources/application.yaml</file>
    <file>docker-compose.yaml</file>
    <file>backend/src/main/java/com/platform/app/AppApplication.java</file>
    <file>backend/src/main/java/com/platform/app/iam/domain/model/User.java</file>
    <file>backend/src/main/java/com/platform/app/iam/domain/exception/InvalidOtpException.java</file>
    <file>backend/src/main/java/com/platform/app/iam/domain/exception/OtpExpiredException.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/dto/UserRegisteredOtpEvent.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/ports/inbound/VerifyOtpCommand.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/ports/inbound/VerifyOtpUseCase.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/ports/outbound/OtpRepositoryPort.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/ports/outbound/EmailNotificationPort.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/services/RegisterService.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/services/VerifyOtpService.java</file>
    <file>backend/src/main/java/com/platform/app/iam/application/listener/RegistrationOtpEventListener.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthController.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/RestExceptionHandler.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/dto/request/VerifyOtpRequest.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/primary/rest/dto/response/VerifyOtpResponse.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/redis/RedisOtpAdapter.java</file>
    <file>backend/src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/notification/LoggingEmailNotificationAdapter.java</file>
    <file>backend/src/test/java/com/platform/app/iam/infrastructure/adapters/primary/rest/AuthControllerTest.java</file>
    <file>backend/src/test/java/com/platform/app/iam/application/services/VerifyOtpServiceTest.java</file>
  </allowed_files>

  <forbidden_files>
    <file>backend/src/main/resources/db/**</file>
    <file>frontend/**</file>
    <file>ai/**</file>
  </forbidden_files>
</scope_contract>

---

## 3. Execution Plan (Compact Slices)

<execution_plan>
  ### Slice 1: Infrastructure & Ports Setup (Redis, Config, Outbound Ports)
  - **Action:** Thêm Redis dependency vào `build.gradle`, thêm redis service vào `docker-compose.yaml` và cấu hình `application.yaml`. Định nghĩa Outbound Ports `OtpRepositoryPort` và `EmailNotificationPort`. Tạo `RedisOtpAdapter` và `LoggingEmailNotificationAdapter`.
  - **Verifier:** ./gradlew compileJava
  - **Status:** [x] DONE

  ### Slice 2: Application Flow (OTP Generation, Async Event, Verification Service)
  - **Action:** Cập nhật `User.java` (thêm `enable()`), tạo domain exceptions (`InvalidOtpException`, `OtpExpiredException`). Cập nhật `RegisterService` tạo tài khoản `enabled = false`, lưu OTP vào Redis và trigger event bất đồng bộ. Tạo `RegistrationOtpEventListener` và `VerifyOtpService`.
  - **Verifier:** ./gradlew compileJava
  - **Status:** [x] DONE

  ### Slice 3: REST API & Exception Handling (Controller, DTOs, Handler)
  - **Action:** Tạo `VerifyOtpRequest`, `VerifyOtpResponse`. Bổ sung endpoint `POST /api/v1/auth/verify-otp` trong `AuthController`. Bổ sung ExceptionHandler trong `RestExceptionHandler`.
  - **Verifier:** ./gradlew compileJava
  - **Status:** [x] DONE

  ### Slice 4: Testing & Verification
  - **Action:** Viết integration test trong `AuthControllerTest` và unit test trong `VerifyOtpServiceTest` kiểm thử luồng đăng ký -> nhận OTP -> xác thực kích hoạt tài khoản thành công -> thử đăng nhập; kiểm thử OTP sai hoặc hết hạn.
  - **Verifier:** ./gradlew test && ./gradlew check
  - **Status:** [x] DONE
</execution_plan>

---

## 4. Consolidated Verification & Gates

<verification_gates>
  <!-- Gate G1/G2: Scope & Architecture check (pre-execution) -->
  - [x] **Gate G1/G2 (Plan Approved):** Allowed files confirmed, test verifiers defined. Approved by engineer.

  <!-- Gate G3: Verification evidence (post-execution) -->
  - [x] **Gate G3 (Ready for Handoff):**
    - [x] Verifier commands executed cleanly (Zero errors: ./gradlew test, ./gradlew check).
    - [x] Git diff inspected — NO files touched outside `<allowed_files>`.
    - [x] No temporary debug logs or unintended changes.
</verification_gates>

</task_lite>
