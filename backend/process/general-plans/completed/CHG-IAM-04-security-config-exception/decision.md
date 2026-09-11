# Decision: DEC-IAM-04 Exception Declaration in SecurityConfig

<technical_decision task_id="CHG-IAM-04" dec_id="DEC-IAM-04" version="1.0" framework="RIPER-5">

<!-- READ-ONLY PHASE. Present options. PAIR: engineer decides. DELEGATED: agent auto-selects optimal recommendation and advances. -->
<decision_status>
  <phase>INNOVATE</phase>
  <mode>READ-ONLY</mode>
  <decision_owner>@engineer</decision_owner>
  <last_updated>2026-09-11</last_updated>
</decision_status>

---

## 1. Context

<context>
  <task>[task.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/general-plans/active/CHG-IAM-04-security-config-exception/task.md)</task>
  <research_artifact>[research.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/general-plans/active/CHG-IAM-04-security-config-exception/research.md)</research_artifact>
  <constraints>
    - Java 25 / Spring Boot 4.0.7 / Spring Security 7.0.7
    - SecurityFilterChain bean signature must be clean and not declare checked exceptions that cannot be thrown from the method body (Sonar java:S1130, java:S112).
    - Unused imports must be eliminated.
  </constraints>
</context>

---

## 2. Decision Required

<decision_question>
  How should the 'throws Exception' declaration on 'SecurityFilterChain securityFilterChain(HttpSecurity http)' in SecurityConfig be resolved?
</decision_question>

---

## 3. Options

<options>

  <option id="A">
    <approach>Remove 'throws Exception' entirely from 'securityFilterChain(HttpSecurity http)' and remove unused import 'AbstractHttpConfigurer'</approach>
    <advantages>
      - Directly addresses Sonar java:S1130 ("Remove the declaration of thrown exception 'java.lang.Exception', as it cannot be thrown from method's body").
      - In Spring Security 7.x, 'SecurityBuilder.build()' has signature 'O build();' without declaring checked exceptions; all internal chained builder calls throw no checked exceptions.
      - Clean, idiomatic, and minimal change. Zero runtime overhead or unnecessary exception wrapping.
    </advantages>
    <disadvantages>None.</disadvantages>
    <complexity>LOW</complexity>
    <compatibility>Fully backward compatible; callers/Spring bean factory do not expect checked exceptions.</compatibility>
    <concurrency_transaction_risk>None.</concurrency_transaction_risk>
    <testability>Verified by compileJava, spotlessJavaCheck, and existing security integration tests.</testability>
    <maintainability>Excellent; aligns with modern Spring Security standards.</maintainability>
  </option>

  <option id="B">
    <approach>Wrap 'http.build()' in try-catch and throw a custom unchecked runtime exception (e.g., SecurityConfigurationException)</approach>
    <advantages>Catches hypothetical runtime exceptions during build.</advantages>
    <disadvantages>Unnecessary boilerplate; Spring's BeanCreationException already wraps any runtime configuration failures during IoC startup.</disadvantages>
    <complexity>MEDIUM</complexity>
    <compatibility>Full.</compatibility>
    <concurrency_transaction_risk>None.</concurrency_transaction_risk>
    <testability>Requires additional tests for artificial catch block.</testability>
    <maintainability>Increased code clutter without tangible benefit.</maintainability>
  </option>

</options>

---

## 4. Trade-off Matrix

<tradeoff_matrix>

| Criterion | Option A (Direct Removal) | Option B (Wrap with Custom Runtime Exception) |
|---|:---:|:---:|
| Compatibility | 5 | 5 |
| Complexity | 5 | 3 |
| Risk | 5 | 4 |
| Testability | 5 | 4 |
| Maintainability | 5 | 3 |

</tradeoff_matrix>

---

## 5. Recommendation

<recommendation>
  Adopt **Option A**: Remove the 'throws Exception' declaration entirely and eliminate the unused 'AbstractHttpConfigurer' import. Because Spring Security 7's 'HttpSecurity.build()' does not throw checked exceptions, removing the throws clause completely resolves the static analysis violation without unnecessary indirection or wrapper exceptions.
</recommendation>

---

## 6. Implementation Decision

<!-- PAIR mode: Completed by engineer before Gate 1 passes. -->
<engineer_decision>
  <selected_option>Option A</selected_option>
  <rationale>Adopted optimal recommendation via fast-track: Spring Security 7 HttpSecurity.build() throws no checked exceptions, so removing throws Exception completely satisfies Sonar java:S1130 and cleans up dead exception declarations without runtime overhead.</rationale>
  <rejected_alternatives>
    - Option B: Unnecessary boilerplate since no checked exception is thrown.
  </rejected_alternatives>
</engineer_decision>

---

## 7. Constraints Created by This Decision

<constraints_created>
  - 'SecurityConfig.java' must compile cleanly without compiler warnings or Spotless violations.
</constraints_created>

---

## 8. Evidence Still Required

<evidence_required>
  - Verification that './gradlew test' and Spotless checks pass with the modified signature.
</evidence_required>

---

## Gate 1 — Decision Approved

<gate id="G1">
  - [x] All options presented and trade-offs analyzed.
  - [x] Selected option recorded above.
  - [x] No blocking business / schema / security decision remains open.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-11</approved_date>
</gate>

</technical_decision>
