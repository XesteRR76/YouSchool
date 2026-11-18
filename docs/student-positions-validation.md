# Student organization switching: validation

The implementation targets upstream `main` at `c6a8e818367291f5943039c12c3d8f59bf473efd` (0.3.10).
Only positions with `posTypeCode = S` are selectable. Parent and teacher features are outside this change.

## Local checks

- All main Kotlin sources compile with the cached Kotlin 2.0 compiler, Compose and serialization plugins, and Android SDK classpath.
- All 40 JVM tests pass, including 13 position filtering, restoration, and cancellation regressions.
- Android instrumentation test sources compile, including Room migration/isolation, synthetic session protocol, and Compose chooser tests.
- `git diff --check` and the UI source detector pass.

Direct compiler checks do not execute Room's KSP processor or replace an Android build.
The managed shell rejects Gradle's local socket with `java.net.SocketException: Operation not permitted`.
Consequently local Gradle unit-test task execution, lint, APK assembly, emulator screenshots, and instrumentation execution remain unverified.
The included `Validate student position switching` workflow is prepared to run those tasks and retain reports and the debug APK as an artifact. It has not run: the GitHub integration rejected publishing to the fork with `403 Resource not accessible by integration`; shell Git transport cannot resolve GitHub, and no browser host is available.

## Synthetic coverage

- Two organizations with the same student role, one student position, empty or non-student lists, invalid IDs, and a removed saved position.
- Restoring the chosen position after restart and automatic login after HTTP 401, before repeating school requests.
- Offline restart using only an account-specific previously confirmed snapshot.
- Failed PUT before the server changes and an ambiguous PUT after it changes; unavailable reconciliation hides all school data.
- Same lesson/grade/homework/upload IDs in different accounts and positions; replacement/deletion cannot affect another context.
- Non-destructive migrations from database versions 6–9 and quarantined legacy rows with unknown ownership.
- Pending homework survives a switch and replays only in its original position; drafts and grade notification baselines remain separate.
- Old in-flight work is cancelled; queued old work and a late response cannot publish into the new context.
- The chooser identifies schools separately, blocks repeated choices during switching, and offers recovery for an empty/error list.

## Real-account verification required

No real eSchool account with two student organizations is available in this session.
Fixtures are not confirmation that the production server switches organizations successfully.
Keep the PR in draft until the following are verified on an authorized real account:

1. Select each organization and verify school name, person ID, year, timetable, grades, homework, conversations, and recipients.
2. Restart and force session expiry; confirm the selected school is restored before any school data request.
3. Switch during a slow load and an offline homework change; verify old content stays hidden and queued changes return to their original school.
4. Check per-school drafts and notification baselines, including first-load suppression of historical grades.
