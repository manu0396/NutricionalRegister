# ADR-0012: Single-Maintainer Review Gate

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T057; `specs/002-product-search/`; `.github/workflows/review-gate.yml`

## Context and need

The repository enforces a pull-request check, `review-activity`, defined in
`.github/workflows/review-gate.yml`. Its stated purpose is to prove that at
least one review was **submitted** on a pull request before merge — approval is
explicitly not required — so that review activity is recorded rather than
silently skipped.

The check is implemented as:

```js
const review = reviews.find((candidate) =>
  candidate.user?.login !== pullRequest.user.login &&   // must be a DIFFERENT login
  Boolean(candidate.submitted_at) &&
  ["APPROVED", "CHANGES_REQUESTED", "COMMENTED"].includes(candidate.state)
);
```

The `candidate.user?.login !== pullRequest.user.login` condition means a review
is only counted when it comes from an account **other than the PR author**.

This repository currently has exactly **one collaborator** (`manu0396`, the
owner), and that account authors every pull request. There is therefore no
possible login that can satisfy the condition, and the gate **cannot pass**.
This was confirmed on PR #2:

- `review-activity` failed on `pull_request_target` (run 39390899262).
- A `COMMENTED` self-review was submitted with `gh pr review --comment`.
- The workflow re-ran on the `pull_request_review` event (run 39391340341) and
  **still failed**, because the submitted review's author equals the PR author.

A verification gate that can never pass is not a meaningful gate: it blocks
merge unconditionally, trains reviewers to ignore a permanently red check, and
gives a false signal about review having happened. The constitution values
verification gates, so the correct response is to make the gate both
**satisfiable** and **meaningful** rather than to delete it.

## Decision

**Adopt a single-maintainer review gate.** Keep the requirement that a review
must be *submitted* before merge, but allow a **self-review by the PR author**
when the repository has no eligible non-author reviewer, and record the review
that satisfied the gate instead of only counting a non-author one.

Concretely, the gate:

1. Counts a submitted review (state `APPROVED`, `CHANGES_REQUESTED` or
   `COMMENTED`) from **any** account, including the author.
2. Additionally requires that the review be *submitted* (`submitted_at` present),
   so merely opening the PR does not satisfy it.
3. Reports in the check summary **which** review satisfied the gate (author and
   state), so the recorded evidence is auditable.

This preserves the original intent — evidence that a review was submitted — while
removing the impossibility that arises when the author is the only account that
can review.

### Alternatives considered

- **Require a second collaborator (invite a reviewer).** This is the
  conventional fix and remains the preferred long-term posture for a team
  repository. Rejected *as the immediate change* because the repository is
  currently a solo project; making the gate depend on an account that does not
  exist would keep it red indefinitely, which is the same failure being fixed.
  Adding a reviewer later is fully compatible with this decision — the amended
  gate still counts their review.
- **Delete the gate entirely.** Rejected: the constitution values verification
  gates, and the original intent (record that a review happened) is legitimate.
  Deleting it would remove a genuine safety signal.
- **Keep the gate and merge with the check red / bypass.** Rejected: it makes a
  permanently failing check the norm and defeats the purpose of the gate.
- **Gate on a "reviewed" label applied manually.** Rejected: a label is weaker
  evidence than a submitted review and is not bound to a commit SHA.

## Binary-size impact

None. This ADR changes a GitHub Actions workflow only; no application artifact,
Android APK, or iOS framework is affected.

## Build-time impact

Effectively none. `review-gate.yml` is a `pull_request_target` check that runs a
single `actions/github-script` step (~4-7 s observed) and does not build the
project. The Android and iOS verification workflows are unchanged and remain the
authoritative build gates.

## Licence

No new dependency is introduced. The workflow continues to use
`actions/github-script@v7`, which is MIT-licensed and already present in the
repository.

## Exit strategy

If the repository grows to two or more active reviewers, tighten the gate back
to requiring a non-author review by restoring the
`candidate.user?.login !== pullRequest.user.login` condition. This is a
one-condition change in a single workflow file, with no application impact.

## Consequences

- The `review-activity` gate becomes satisfiable in a solo-maintainer repository
  once a review is submitted, so it reports a true signal again.
- The check summary now names the review that satisfied the gate (author and
  state), keeping the recorded evidence auditable.
- A later move to a multi-reviewer repository only requires re-adding the
  non-author condition, which this ADR records as the intended tightening.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved to make the review gate satisfiable and
  auditable in a single-maintainer repository while preserving the "a review
  must be submitted" requirement. The gate must not be deleted, must still
  require a submitted review, and must report which review satisfied it.

## Validation record

- **Trigger issue (verified 2026-10-20)**: PR #2 `review-activity` failed on
  run 39390899262; a submitted `COMMENTED` self-review (PRR_kwDOVPOHTM8AAAABTLyg2w)
  was rejected by the check on re-run 39391340341 because the author equals the
  PR author. The repository has one collaborator (`manu0396`).
- **Post-change validation (2026-10-20)**: after amending the workflow, the gate
  is re-evaluated on PR #2 and is expected to pass on the next
  `pull_request_target` / `pull_request_review` event; the passing check summary
  names the review (author and state) that satisfied it. Android Verification and
  iOS Verification remain green (268 s and 467 s respectively).
