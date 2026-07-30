# Workout Business Flow

This note describes the W1-W9 application-layer behavior. It does not replace the database migration checklist.

## Boundaries

- `WorkoutController` binds the authenticated HTTP contract and returns `ApiResponse<T>`.
- `WorkoutApplicationService` coordinates workspace reads, session lifecycle, cycle progression, history, restart, and the AI placeholder.
- `WorkoutSessionPolicyDomainService` requires every submitted exercise, item, and metric key to exactly match the immutable session snapshot.

## Invariants

1. W2 and W5 use the fixed write-lock order `user_active_cycles -> cycle_runs -> cycle_templates -> training_sessions`. Template activation follows the same order before cancelling in-progress sessions, preventing lock-order inversion with W5.
2. W4 and W5 reject missing, duplicate, or newly introduced exercises, items, and metric keys. Rest-day sessions accept only an empty exercise list.
3. W3 and W5 completed-day responses use `training_sessions.template_name_snapshot` and `day_name_snapshot`; history is not renamed when the current template changes.
4. W5 runs in one transaction: save actual values, complete the session, update `lastSessionId`, then advance the day or mark the run completed.
5. Session detail hydration batches exercises, items, and metrics; W7 batch-loads cycle runs instead of querying one run per record.
6. Completed and cancelled sessions are read-only. W8 only creates a new run when the current run is completed. W9 returns `WORKOUT_AI_ANALYSIS_COMPLETED_CYCLE_REQUIRED` until that precondition is met, then returns `WORKOUT_AI_NOT_IMPLEMENTED`.
7. Activating another template only cancels the old active run and its in-progress sessions. Completed sessions remain unchanged.
8. Saving an active template refreshes the current Day in-progress session after the frontend warns the user, receives confirmation, and sends confirmOverwriteCurrentSession=true. The refresh replaces session notes, exercise states, failure reasons, exercise feedback, and actual metric values; completed and cancelled sessions remain immutable.
