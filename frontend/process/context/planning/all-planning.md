# Planning Standards & Calibration Context

<planning_context version="1.0">

<overview>
  Guidelines for story point estimation, vertical slicing, and capacity calibration.
</overview>

## 1. Quality Standards
<quality_standards>
  <standard name="INVEST">
    - **Independent:** Deliverable without cross-story blockers.
    - **Negotiable:** Implementation details are flexibly refined.
    - **Valuable:** Delivers measurable user or business value.
    - **Estimable:** Scoped clearly to estimate effort.
    - **Small:** Fits within 1-3 engineering days.
    - **Testable:** Concrete Acceptance Criteria defined.
  </standard>

  <standard name="Vertical Slicing">
    Avoid horizontal silos. Deliver end-to-end vertical slices across API, logic, and persistence.
  </standard>
</quality_standards>

## 2. Capacity Calibration
<capacity_calibration>
  <unit>1 Story Point ≈ 2-4 focused engineering hours</unit>
  <max_task_size>3-5 Story Points (larger tasks must be decomposed)</max_task_size>
</capacity_calibration>

</planning_context>
