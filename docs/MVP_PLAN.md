# Historical MVP implementation plan

This file records the original plan, not a current release number or feature contract.
For current behavior and validation see [AUDIT_REPORT](AUDIT_REPORT.md) and [ARCHITECTURE](ARCHITECTURE.md).

1. Establish the verified dependency matrix and source-backed analysis.
2. Ship localized, persistent settings and safe client/server event boundaries.
3. Collect frame, server tick, heap and GC metrics without hot-path allocation.
4. Provide incremental PC benchmark and a base profile.
5. Provide bounded lag diagnostics with explicit evidence labels.
6. Generate user-reviewed rule-based recommendations.
7. Implement the distant belt-item shadow optimization and counters.
8. Provide a real runtime A/B session; do not pre-populate results.
9. Build, test localization parity, and exercise client/dedicated-server startup.
