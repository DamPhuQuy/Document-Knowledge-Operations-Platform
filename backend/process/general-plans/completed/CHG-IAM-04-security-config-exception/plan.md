# Plan: PLN-IAM-04 Remove Thrown Exception Declaration in SecurityConfig

<execution_plan task_id="CHG-IAM-04" plan_id="PLN-IAM-04" version="2.0" framework="RIPER-5">

<!-- PLAN PHASE. Plan artifacts only. No source-code changes. -->
<plan_status>
  <phase>PLAN</phase>
  <last_updated>2026-09-11</last_updated>
</plan_status>

---

## 1. Input Artifacts

<input_artifacts>
  <task_spec>[task.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/general-plans/active/CHG-IAM-04-security-config-exception/task.md)</task_spec>
  <research>[research.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/general-plans/active/CHG-IAM-04-security-config-exception/research.md)</research>
  <decision>[decision.md](file:///home/phuqy/Develop/Document-Knowledge-Operations-Platform/backend/process/general-plans/active/CHG-IAM-04-security-config-exception/decision.md) — DEC-IAM-04, Option A</decision>
</input_artifacts>

---

## 2. Execution Constraints

<execution_constraints>
  <allowed_files>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java` — Remove throws Exception and unused import
  </allowed_files>
  <forbidden_files>
    - `src/main/java/com/platform/app/iam/domain/**` — Domain must not be touched
    - `src/main/java/com/platform/app/iam/application/**` — Application layer untouched
    - `src/main/resources/**` — Config and migration untouched
  </forbidden_files>
  <allowed_commands>
    - `./gradlew compileJava`
    - `./gradlew test`
    - `./gradlew check`
    - `./gradlew spotlessApply`
  </allowed_commands>
  <restricted_operations>
    - No DB migrations without explicit engineer approval.
    - No new third-party dependencies without explicit engineer approval.
  </restricted_operations>
  <required_approvals>
    - Fast-track auto-certified.
  </required_approvals>
</execution_constraints>

---

## 3. Slice Summary

<slice_summary>

| ID | Behavior | Boundary | Files | AC | Verifier | Risk | Mode | Rollback |
|---|---|---|---|---|---|---|---|---|
| S1 | Remove throws Exception and unused import | Infrastructure / Security Config | SecurityConfig.java | AC-1, AC-2, AC-3 | `./gradlew test` & `./gradlew check` | LOW | EXECUTE | git checkout |

</slice_summary>

---

## 4. Slice Details

<slices>

  <slice id="S1">
    <objective>Remove throws Exception and unused import in SecurityConfig</objective>
    <change>Update securityFilterChain method signature to remove 'throws Exception' and delete unused import 'AbstractHttpConfigurer'</change>
    <allowed_files>
      - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java`
    </allowed_files>
    <acceptance_criteria>
      - [ ] AC-1: 'securityFilterChain(HttpSecurity http)' in SecurityConfig has the 'throws Exception' clause removed.
      - [ ] AC-2: Unused imports in SecurityConfig are cleaned up.
      - [ ] AC-3: The project compiles cleanly and './gradlew check' / './gradlew test' pass without regressions.
    </acceptance_criteria>
    <verifier>
      ```bash
      ./gradlew compileJava
      ./gradlew check
      ./gradlew test
      ```
    </verifier>
    <expected_evidence>BUILD SUCCESSFUL with 0 compile errors, 0 spotless violations, and all tests passing</expected_evidence>
    <rollback_point>git checkout -- src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java</rollback_point>
    <stop_conditions>
      - Compilation fails due to unexpected checked exception requirement.
    </stop_conditions>
  </slice>

</slices>

---

## 5. Scope Contract

<scope_contract>
  <allowed>
    - `src/main/java/com/platform/app/iam/infrastructure/adapters/secondary/security/config/SecurityConfig.java`
  </allowed>
  <forbidden>
    - Any domain or application layer files.
    - Any database migrations or yaml configurations.
  </forbidden>
</scope_contract>

---

## 6. Verification Matrix

<verification_matrix>

| AC / Risk | Test / Command | Expected Evidence | Actual Result |
|---|---|---|---|
| AC-1 | `./gradlew compileJava` | Clean compilation | PASS |
| AC-2 | `./gradlew check` | Spotless check passes | PASS |
| AC-3 | `./gradlew test` | All unit & integration tests pass | PASS |

</verification_matrix>

---

## Gate 2 — Plan Approved

<gate id="G2">
  - [x] Every slice has a defined verifier.
  - [x] Scope contract (allowed / forbidden) approved.
  - [x] Stop conditions defined per slice.
  - [x] Rollback point defined per slice.
  - [x] Allowed commands listed.
  - [x] Plan approved.
  <approved_by>[AUTO: DELEGATED]</approved_by>
  <approved_date>2026-09-11</approved_date>
</gate>

</execution_plan>
