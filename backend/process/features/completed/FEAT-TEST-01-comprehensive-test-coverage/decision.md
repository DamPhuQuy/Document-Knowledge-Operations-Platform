# Decision: DEC-TEST-01 Testing Architecture and Slice Decomposition Strategy

<technical_decision task_id="FEAT-TEST-01" dec_id="DEC-TEST-01" version="1.0" framework="RIPER-5">

<decision_status>
  <phase>INNOVATE</phase>
  <mode>DELEGATED</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-19</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>[FEAT-TEST-01 Comprehensive Unit and Slice Testing Suite](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/FEAT-TEST-01-comprehensive-test-coverage/task.md)</task>
  <research_artifact>[research.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/features/active/FEAT-TEST-01-comprehensive-test-coverage/research.md)</research_artifact>
  <constraints>
    - Hệ thống có 175 files trong `src/main/java`.
    - Phải đảm bảo nguyên tắc Clean Architecture: Domain tách biệt, Application dùng mock, Infrastructure dùng slice test thích hợp.
    - Không gây chậm thời gian build test (tối đa hóa tốc độ chạy).
    - Tuân thủ yêu cầu người dùng: Mỗi file tương ứng trong src đều có kiểm thử tương ứng.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  Lựa chọn kiến trúc phân tầng kiểm thử và chiến lược phân bổ slice nào để đảm bảo độ phủ 1:1, tốc độ thực thi cao nhất và bảo trì lâu dài?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>
      Kiến trúc Clean Testing Layered & Slice Phân Rã 1:1 (Khuyến nghị):
      - Tầng Domain (Models, Enums): Pure POJO Unit Tests (không Spring context, chạy dưới 5ms).
      - Tầng Application (Services, Listeners): Pure Mockito Unit Tests với Inbound/Outbound port mocking chặt chẽ.
      - Tầng Infrastructure Primary (Controllers, ExceptionHandler): @WebMvcTest slice tests với MockMvc.
      - Tầng Infrastructure Secondary (Persistence Adapters, Redis, Storage, Security Filters): Tách riêng từng adapter thành test file độc lập 1:1 (Unit test hoặc @DataJpaTest cho JPA specification/repositories).
    </approach>
    <advantages>
      - Tốc độ thực thi cực nhanh (hầu hết là pure Java/Mockito tests).
      - Cách ly lỗi chuẩn xác: Lỗi ở adapter nào thì test adapter đó fail, không ảnh hưởng lẫn nhau.
      - Phản ánh đúng 1:1 cấu trúc mã nguồn dự án.
    </advantages>
    <disadvantages>
      - Số lượng file test nhiều hơn (cần tạo mới ~15-20 file test).
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Tương thích 100% với Spring Boot 4.0 và Gradle hiện tại</compatibility>
    <concurrency_transaction_risk>Không có (các bài test hoàn toàn độc lập, không dùng shared mutable state)</concurrency_transaction_risk>
    <testability>Rất cao (99-100% path coverage trên các class mới)</testability>
    <maintainability>Rất cao</maintainability>
  </option>

  <option id="B">
    <approach>
      Sử dụng @SpringBootTest tải full context cho mọi thành phần.
    </approach>
    <advantages>
      - Test được toàn bộ bean injection và cấu hình tổng thể.
    </advantages>
    <disadvantages>
      - Chậm nghiêm trọng (Spring Boot context reloading nhiều lần tốn hàng phút).
      - Khó mock các thành phần ngoại vi như AWS S3 hoặc Redis Cluster.
      - Vi phạm nguyên tắc F.I.R.S.T của unit testing.
    </disadvantages>
    <complexity>HIGH</complexity>
    <compatibility>Cao nhưng tốn tài nguyên</compatibility>
    <concurrency_transaction_risk>Nguy cơ xung đột DB rollback giữa các thread</concurrency_transaction_risk>
    <testability>Trung bình (khó tái hiện edge case lỗi)</testability>
    <maintainability>Thấp</maintainability>
  </option>

  <option id="C">
    <approach>
      Chỉ bổ sung test cho 3 Application Services thiếu sót (`RegisterService`, `DocumentMetadataService`, `RegistrationOtpEventListener`) và bỏ qua Domain Models / Adapters còn lại.
    </approach>
    <advantages>
      - Khối lượng thực hiện ít nhất.
    </advantages>
    <disadvantages>
      - Vi phạm yêu cầu rõ ràng của người dùng: "viết test toàn diện với mỗi file tương ứng trong src sẽ viết trong test".
      - Để lại lỗ hổng kiểm thử ở Security Filters, Exception Handler và Secondary Adapters.
    </disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Cao</compatibility>
    <concurrency_transaction_risk>Không</concurrency_transaction_risk>
    <testability>Thấp (bỏ sót nhiều lớp)</testability>
    <maintainability>Thấp</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Clean Slice 1:1) | Option B (Full Context) | Option C (Minimal Services Only) |
|---|:---:|:---:|:---:|
| Compatibility | 5 | 4 | 5 |
| Execution Speed | 5 | 1 | 5 |
| Coverage Rigor (1:1) | 5 | 3 | 2 |
| Isolation & Determinism | 5 | 2 | 4 |
| Maintainability | 5 | 2 | 3 |

</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Chọn **Option A**. Đây là phương án tối ưu toàn diện, đáp ứng đầy đủ yêu cầu "viết test toàn diện với mỗi file tương ứng trong src", đảm bảo tốc độ chạy của `./gradlew test` luôn dưới 30 giây, tuân thủ nguyên tắc Clean Architecture và F.I.R.S.T.
</recommendation>

---

## 6. Implementation Decision

<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>[AUTO: DELEGATED] Người dùng đã phê duyệt và yêu cầu fast-track. Option A đảm bảo độ phủ 1:1 cho tất cả các thành phần cốt lõi còn thiếu theo phân tầng Clean Architecture mà không gây suy giảm hiệu năng bộ test.</rationale>
  <rejected_alternatives>
    - Option B: Bị loại bỏ vì chi phí khởi tạo Spring Context quá lớn và dễ gây flaky test.
    - Option C: Bị loại bỏ vì không đáp ứng yêu cầu kiểm thử toàn diện của người dùng.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - Các bài test Application Service phải sử dụng `@ExtendWith(MockitoExtension.class)`, không khởi động Spring ApplicationContext.
  - Các bài test Security Filters sử dụng `MockHttpServletRequest` và `MockHttpServletResponse`.
  - Các bài test Exception Handler kiểm tra toàn diện mapping của các Domain Exceptions (`InvalidCredentialsException`, `DocumentAccessDeniedException`, `EmailAlreadyExistsException`, v.v.).
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Xác nhận tất cả test mới viết compile thành công và pass 100% trên Java 25.
  - `./gradlew test` và `./gradlew check` chạy thành công không vi phạm spotless linter.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-19</approved_date>
</gate>

</technical_decision>
