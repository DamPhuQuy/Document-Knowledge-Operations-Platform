# Decision: DEC-IAM-03 Adopt ConfigurationProperties for JWT Security Configuration

<technical_decision task_id="CHG-IAM-03" dec_id="DEC-IAM-03" version="1.0" framework="RIPER-5">

<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-11</last_updated>
</decision_status>

---

## 1. Context & Decision

<context>
  Replace dispersed `@Value` constructor injections with strongly-typed `@ConfigurationProperties` for `app.jwt`.
</context>

## 2. Decision Required

<decision_question>
  What pattern should be used for `JwtProperties`?
</decision_question>

## 3. Options

<options>
  - Option A (Selected): Java Record with `@ConfigurationProperties(prefix = "app.jwt")` and `@Validated` + Jakarta validation (`@NotBlank`, `@Size(min = 32)`, `@Positive`).
  - Option B: Mutable POJO class with Lombok `@Data`.
</options>

## 4. Engineer Decision & Gate 1 Sign-Off

<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>Java Record provides native immutability and concise syntax without Lombok boilerplate. Bean validation enables fail-fast checks at startup.</rationale>
</engineer_decision>

<gate id="G1" label="Gate 1 — Decision Approved">
  <status>APPROVED</status>
  <approved_by>@engineer</approved_by>
  <approved_date>2026-09-11</approved_date>
</gate>

</technical_decision>
