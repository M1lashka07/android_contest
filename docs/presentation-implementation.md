# Presentation implementation

Created 2026-09-30. Kotlin and Jetpack Compose; `CombatApp` depends only on the `CombatRepository` domain interface. `CombatViewModel` coordinates requests and retained state. Reusable themed controls come from the independent UIKit project.

## Implemented flows

- First-install three-page onboarding, swipe and tappable page indicators, skip, persisted completion.
- Registration and login, the required lowercase alphanumeric `.ru` email validation, password visibility and confirmation. Password text is kept only in the composable's memory and never written to disk. Country code and phone are separate fields.
- Landing, animated swipe-dismissable drawer, bottom navigation with original Figma SVG artwork.
- Scheduling with Android date/time pickers, category, points, description, optional notification permission and server publication. Success is shown after `createCombat` succeeds.
- Discover search/category/status filters, game details, atomic server join, common server start time, opponent polling, player profiles and current-player statistics.
- Profile edit and external camera/gallery photo selection. Images are resized and encoded through the platform helper before upload.
- Seeded Image sliding puzzle and Circle ordering puzzle. Every accepted move is recorded for the server to validate. Completion automatically submits the replay. Only a confirmed `FINISHED` server response clears the active-game marker.
- Five-second dismissable error banner, loading indicators, server error logging, and screen/move/result analytics hooks.

## Lifecycle and persistence

The view model survives configuration changes. SharedPreferences persist the last route, selected game/player, onboarding page, filters, and non-secret form drafts. The data layer separately owns encrypted session persistence.

Leaving an active game records an account-owned pending defeat before attempting a network request. A background app stops combat polling and sends the defeat. A late start response received while the app is hidden also records a defeat. After process death, a persisted active marker is treated as leaving the match. Pending defeats are keyed by user ID; another account cannot submit them. They are retried on return, next start, and logout. Logout does not discard the session while its defeat cannot be acknowledged. Background presentation requests are cancelled when the session ends.

The server separately adjudicates a missing heartbeat. Android process termination cannot be relied upon to execute a final network call.

## Design provenance and limitations

Onboarding, auth, landing, drawer and scheduling use the available high fidelity source contexts and original local assets documented in [design.md](design.md). The Android system status/navigation bars replace the source iPhone mock bars. Player photographs remain dynamic rather than using the checkerboard reference avatars.

Success, Discover details, player/profile edit, statistics, Image/Circle gameplay and results extend the supplied Poppins/pink design language because the required source frames/prototype could not be retrieved. These screens must not be described as pixel-matched to unavailable designs. The small plain gray rectangle over the Create Account source illustration is omitted as a source artifact.

The source's social login and password-recovery actions explain their missing service configuration. The Chat navigation entry currently opens the player directory; messaging/audio/video are outside the eleven specified backend operations and are not implemented. The onboarding marketing copy comes from the source; there is no cash withdrawal implementation. Publisher-approved privacy and agreement PDFs must be supplied separately.

## Automated presentation checks

`app/src/androidTest/.../CombatFlowTest.kt` defines three Compose instrumentation checks:

1. First-launch skip opens registration and persists onboarding completion.
2. An invalid competition email never invokes the repository's authentication methods.
3. Publishing sends a `ScheduleRequest` through the abstraction before showing success.

The repository used by these tests lives only in `androidTest`. These checks verify presentation integration, not real Supabase end-to-end connectivity or two-device gameplay. Actual execution results belong in the build verification report; the existence of test source alone is not evidence of a pass.
