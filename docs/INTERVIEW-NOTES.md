# Interview notebook

Use this notebook for explanations the learner has practiced and reviewed.
An implemented feature is evidence to discuss, not proof of understanding.

## Answer structure

1. **Problem:** What requirement or observed failure motivated the change?
2. **Mechanism:** What happens at runtime, and which component owns it?
3. **Failure:** Under what conditions does the behavior break or become surprising?
4. **Tradeoff:** Why this design, and when would another choice be better?
5. **Evidence:** Which experiment, test, SQL statement, or metric supports the claim?

Aim for a clear two-minute explanation, then answer deeper follow-up questions.
Distinguish what this project demonstrates from production experience.

## Lesson 001 — application bootstrap and HTTP endpoint

Status: awaiting the learner's explanation.

- What happens when `OrderflowApplication.main` runs?
- How does a request reach the method serving `/api/learning/status`?
- How does the method's return value become JSON?
- Which behavior comes from the application code and which from Spring Boot?
- What would you inspect first if the endpoint returned 404?

Learner's answer, reviewed corrections, and supporting observations: pending.
