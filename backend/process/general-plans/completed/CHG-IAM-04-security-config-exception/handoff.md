# Handoff: CHG-IAM-04

<handoff task_id="CHG-IAM-04" version="2.0" framework="RIPER-5">

<!-- Final projection. Short. Do not duplicate research/plan/review artifacts. -->
<!-- Answer: What changed? Why? What proves it? What remains risky? -->

<handoff_status>
  <review_decision>PASS</review_decision>
  <review_artifact>[review.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/general-plans/completed/CHG-IAM-04-security-config-exception/review.md)</review_artifact>
  <completed_date>2026-09-11</completed_date>
</handoff_status>

---

## 1. What Changed

<what_changed>
  Removed the declared thrown exception 'throws Exception' from 'securityFilterChain(HttpSecurity http)' in SecurityConfig and cleaned up unused imports, ensuring compliance with Sonar static analysis rules (java:S1130 / java:S112).
</what_changed>

<main_changes>
  - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java` — Removed throws Exception from securityFilterChain bean method.
</main_changes>

---

## 2. Why

<why>
  In Spring Security 7.x (Spring Boot 4.x), 'HttpSecurity.build()' does not declare any thrown checked exceptions, and none of the chained configuration calls throw checked exceptions. Declaring 'throws Exception' was redundant dead code flagged by static analysis.
</why>

---

## 3. What Proves It

<evidence>

| AC | Verifier | Result |
|---|---|---|
| AC-1 | `./gradlew compileJava` | PASS |
| AC-2 | `./gradlew check` | PASS |
| AC-3 | `./gradlew test` | PASS |

<!-- To reproduce: -->
```bash
./gradlew check
./gradlew test
```

</evidence>

---

## 4. What Remains Risky

<residual_risk>
  None.
</residual_risk>

---

## 5. Decisions & Assumptions

<decisions_and_assumptions>
  - DEC-IAM-04: Option A chosen — direct removal of 'throws Exception' without wrapper runtime exceptions, as Spring Boot automatically wraps bean initialization failures in BeanCreationException.
</decisions_and_assumptions>

---

## 6. Next Action

<next_action>
  NONE.
</next_action>

</handoff>
