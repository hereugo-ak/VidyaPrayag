# Loop Review Prompt — Comprehensive Review of Current Feature Development

> **How to use:** Tell your AI agent: *"Refer to `LOOP_REVIEW_PROMPT.md` and execute it."*
> The agent will run the full review below **5 times in a loop**, each iteration stricter and more thorough than the last. Issues found in earlier iterations must be verified as fixed (or still present) in later iterations. The final output is a single consolidated report.
>
> **Scope:** The review targets **uncommitted changes and current feature development** — not the entire codebase. Start by running `git diff` and `git status` to identify all modified, added, and untracked files. Focus the review on those files and their direct interactions with the rest of the app.

>
> **End Task After 5 iteration:** Build the server and app, run the app and verify that it works as expected. If some issues are found, fix them and repeat the process.
---

## Execution Rules

1. **Run exactly 5 iterations.** Label each iteration clearly (Iteration 1/5, 2/5, …, 5/5).
2. **Iteration 1** — Baseline review. First, run `git diff` and `git status` to identify all uncommitted changes. Then cover every category below at a "first pass" depth, **scoped to the changed files and their immediate dependencies**.
3. **Iterations 2–4** — Deepen the review. Re-verify every issue from the previous iteration. If an issue was fixed between iterations, mark it **Resolved**. If still present, escalate its severity. Look for new issues that were missed earlier.
4. **After each iteration (1–4), FIX all issues found before starting the next iteration.** Apply the recommended fix for every issue identified in that iteration — edit the code, verify the change, and ensure the fix doesn't introduce new problems. Only move to the next iteration once all issues from the current iteration are resolved.
5. **Iteration 5** — Final pass. Treat the app as if it ships to millions of users tomorrow. Be ruthlessly strict. No issue is too small. Produce the **Final Consolidated Report**.
6. **Between iterations**, do NOT skip categories. Each iteration must re-examine all 10 categories.
7. **Output after each iteration:** A short summary of new findings, issues fixed (with before/after), and any escalated issues.
8. **Output after Iteration 5:** The full Final Report (see Section 10).

---

## Review Scope

Perform a comprehensive review of **all uncommitted changes and current feature development** as if you are a **Senior Staff Engineer, QA Engineer, UI/UX Designer, and Product Reviewer**.

Start by running `git diff` (staged + unstaged) and `git status` to identify every modified, added, and untracked file. Focus the review on:
- Every changed file and its code quality, architecture, business logic, API integration, UI, UX, and user flows.
- How the changed files interact with existing code (imports, dependencies, navigation, state).
- Whether the changes introduce regressions or break existing flows.

**Do not assume anything works — verify every part of the changed code.**

---

### 1. Code Review

- Review every modified, added, or untracked file identified by `git diff` / `git status`.
- Check for bugs, edge cases, crashes, memory leaks, race conditions, nullability issues, and improper state handling.
- Ensure code follows best practices, clean architecture, MVVM, SOLID principles, and is maintainable.
- Identify duplicated logic, dead code, unused resources, and optimization opportunities.
- Suggest improvements wherever applicable.

---

### 2. Flow Review

- Test every user journey from start to finish.
- Verify navigation between all screens.
- Ensure loading, success, empty, and error states are handled correctly.
- Check back navigation, deep links (if applicable), and state restoration.
- Verify no broken flows or inaccessible screens exist.

---

### 3. API Review

Verify every API integration:

- Every API is actually called.
- Correct request payloads.
- Correct response parsing.
- Error handling.
- Timeout handling.
- Retry logic (if applicable).
- Authentication.
- Token refresh.
- Loading indicators.
- Empty states.
- Offline handling.
- No hardcoded values.
- No missing endpoints.
- Ensure every API is connected to the UI correctly.

---

### 4. UI Review

Inspect every screen for:

- Alignment
- Margins
- Padding
- Spacing consistency
- Typography
- Font weights
- Colors
- Icons
- Image scaling
- Elevation
- Shadows
- Borders
- Corner radius
- Responsive layouts
- Different screen sizes
- Landscape support (if required)
- Dark mode (if supported)
- Accessibility
- Touch target sizes
- Scroll behavior
- Clipping
- Overflow
- Animation smoothness
- Keyboard interactions
- Status bar / navigation bar appearance

Flag even the smallest visual inconsistency.

---

### 5. UX Review

Evaluate whether:

- The app feels intuitive.
- User actions are obvious.
- Feedback is provided after interactions.
- Error messages are helpful.
- Empty states are meaningful.
- Loading states improve perceived performance.
- Animations feel natural.
- User friction is minimized.
- The experience feels polished and production-ready.

---

### 6. Functional Testing

Verify:

- Buttons
- Click actions
- Forms
- Validation
- Search
- Filters
- Lists
- Pagination
- Refresh
- Navigation
- Dialogs
- Bottom sheets
- Permissions
- Camera/Gallery (if applicable)
- Authentication
- Logout
- Session handling
- Background/foreground behavior

Every interactive element should be tested.

---

### 7. Performance Review

Check for:

- Unnecessary recompositions
- Expensive UI operations
- Network inefficiencies
- Memory usage
- Image loading optimization
- Lazy loading
- State management issues
- Jank
- ANR risks
- Startup performance

---

### 8. Security Review

Verify:

- Sensitive data isn't exposed.
- Tokens are stored securely.
- No secrets are hardcoded.
- Inputs are validated.
- API calls are secure.
- Logging doesn't expose confidential information.

---

### 9. Production Readiness

Check whether the app is ready for release:

- No placeholder UI.
- No TODOs.
- No debug code.
- No fake data.
- No unfinished features.
- No broken navigation.
- No inconsistent UI.
- No crashes.

---

### 10. Final Report (produced after Iteration 5)

Produce a structured report with:

1. **Critical Issues (Must Fix)**
2. **Major Issues**
3. **Minor Issues**
4. **UI/UX Improvements**
5. **Performance Improvements**
6. **Code Quality Improvements**
7. **Architecture Suggestions**
8. **Security Concerns**
9. **Release Blockers**
10. **Overall Production Readiness Score (0–100)**

---

## Strictness Directive

Be extremely strict and detail-oriented. Review the application as if it will be released to millions of users tomorrow. Do not overlook small spacing issues, inconsistent padding, misaligned components, broken states, missing error handling, unreachable code, or edge cases.

**Every issue must include:**
- **Location** — file path, screen name, and line number(s).
- **Why it matters** — the risk or user impact.
- **Impact severity** — Critical / Major / Minor.
- **Recommended fix** — actionable, specific steps.

The goal is to deliver a polished, production-grade application with no functional, visual, or architectural defects.
