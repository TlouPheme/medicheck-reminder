# Use of generative AI in Part 3

## Tool used

Part 3 was built with **Claude Code**, Anthropic's coding assistant (Claude Opus 5.5), on Tlou Pheme's computer (Anthropic, 2026). When asked, it edits project files, runs Gradle, Git and the GitHub CLI, and reads the phone's logs over USB.

## How it was used

Tlou directed the work one request at a time. Claude Code then wrote most of the Part 3 code, tests and documentation:

- **Sign-in:** Google sign-in through Firebase Authentication and Credential Manager, with email and Google users linked to one Firestore record.
- **Offline mode:** Room changes with a database migration, a WorkManager and Firestore sync using last-write-wins, and the offline banner.
- **Refill calculator:** user-defined feature 4.
- **Push notifications and the full-screen alarm:** Firebase Cloud Messaging, plus the escalating full-screen alarm (user-defined feature 5).
- **Fixes to Ayanda's profile-photo branch:** merge conflicts, an icon typo, and the switch from anonymous sign-in to the real Firebase user.
- **Tooling and release:** the GitHub Actions workflow, 66 JUnit tests, the signed release build, the store listing, the privacy policy, the README and the release notes.
- **This write-up:** Claude Code drafted it, and Tlou checked it.

Ayanda wrote the profile-photo feature, launcher icon and store graphics herself, on her own GitHub account.

Angel and Hlompho could not complete their parts because of slow devices and unreliable internet. Their areas, and Lerato's offline sync and refill calculator, were built through Claude Code on Tlou's GitHub account, so those commits appear under Tlou.

## How we checked the output

AI output was not accepted without checking:

- **Builds and tests.** Every change was compiled and the unit tests run locally before committing. GitHub Actions runs them again on each push and pull request.
- **A real phone.** Features were tried on a physical OnePlus phone running Android 16, with the app's own logs (`Log.d`/`Log.e`) used to confirm each step. For example: Google sign-in creating the `users/{uid}` record, a Taken dose reducing stock from 8 to 7 and raising the refill reminder, and offline changes reaching Firestore.
- **Bugs caught.** Testing found real mistakes in AI-written work: an unescaped apostrophe that broke the resource build, wrong Android SDK package names in CI, and a duplicate Gradle plugin line. Each was fixed and re-tested.
- **Translations.** Claude Code wrote the isiZulu and Afrikaans text. They still need a check by a first-language speaker in the group.

## Referencing

- **Tool.** Claude Code is cited below.
- **Adapted code.** Code following official documentation has the source URL in a comment above it (Firebase, Credential Manager and WorkManager guides).
- **Commits.** Commits written with Claude Code carry a `Co-Authored-By: Claude` line, so the AI's share is visible in Git.

## Reference list

Anthropic (2026) *Claude Code* [AI coding assistant, Claude Opus 5.5]. Available at: https://claude.com/claude-code (Accessed: 5 November 2026).
