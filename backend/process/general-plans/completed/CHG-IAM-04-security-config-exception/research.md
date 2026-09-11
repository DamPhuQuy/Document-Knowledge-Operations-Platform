# Research: CHG-IAM-04 Remove Thrown Exception Declaration in SecurityConfig

<research_context task_id="CHG-IAM-04" version="1.0" framework="RIPER-5">

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
  In `SecurityConfig.java`:
  ```java
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
  ```

  ### Confirmed Findings:
  1. `SecurityBuilder.build()` in Spring Security 7.0.7 (Spring Boot 4.0.7) has signature `public abstract O build();` without declaring `throws Exception`.
  2. In earlier Spring Security versions (e.g. 5.x), `SecurityBuilder.build()` declared `throws Exception`, which commonly led developers to write `throws Exception` on `@Bean public SecurityFilterChain securityFilterChain(...)`.
  3. In Spring Security 7.x, neither `build()` nor any of the fluent configuration calls (`csrf`, `cors`, `sessionManagement`, `authorizeHttpRequests`) throw any checked exceptions.
  4. Static analysis rules (such as SonarQube `java:S1130` - "Methods should not declare thrown checked exceptions they do not throw" and `java:S112` - "Generic exceptions should never be thrown") flag this unnecessary declaration.
  5. There is an unused import: `import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;` which can also be cleaned up.
</current_behavior>

---

## 2. Invariants & Guardrails

<invariants>
  1. The security bean definition must remain a valid Spring `@Bean` returning `SecurityFilterChain`.
  2. No changes to the actual security rules, permitAll endpoints, CORS configuration, or CSRF settings.
  3. All existing unit and integration tests must pass cleanly.
</invariants>

---

## 3. Exit Criteria

<exit_criteria>
  - [x] Baseline behavior and bytecode/interface signatures verified (Spring Security 7.x `SecurityBuilder.build()` does not throw `Exception`).
  - [x] Target file identified (`SecurityConfig.java`).
  - [x] Unused import identified (`AbstractHttpConfigurer`).
  - [x] Ready to advance to INNOVATE phase.
</exit_criteria>

</research_context>
