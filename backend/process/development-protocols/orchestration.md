# Subagent Delegation & Orchestration Protocol

<orchestration_protocol version="1.0">

<description>
  Guidelines for delegating, isolating, and coordinating subagents during complex tasks.
</description>

## 1. Delegation Principles
<delegation_rules>
  <rule id="explicit_scope">
    Always specify exact files, function signatures, and expected structured output formats.
  </rule>

  <rule id="context_containment">
    Do not grant unbounded repo access; constrain subagent context to relevant test suites and port contracts.
  </rule>

  <rule id="no_polling">
    Rely on the system's reactive message wakeup mechanism when subagents complete their execution.
  </rule>
</delegation_rules>

</orchestration_protocol>
