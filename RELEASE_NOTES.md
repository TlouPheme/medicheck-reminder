# MediCheck Reminder — Release Notes

Changes since the Part 2 prototype. Items marked **★ Innovative feature** come from the Part 1 design document.

## v1.0-final-poe (Part 3)

### Sign-in (Tlou)
- **Google sign-in (SSO).** Added a "Sign in with Google" button. It uses Android Credential Manager and Firebase Authentication.
- **One user record for both sign-in methods.** Email sign-ups now also create a Firebase Auth account. Email and Google users with the same address share one `uid` and one Firestore record at `users/{uid}`.
- **Sign in on a new phone.** An account made on another phone can now sign in with its email and password. It is then saved locally, so later logins work offline.
- **Sign-in errors.** A cancelled account picker, no Google account, no network, and a timeout are each handled without a crash, with a message in English, isiZulu, and Afrikaans.
- **Sign-out.** Signing out now also ends the Firebase session and clears the remembered Google account.

### Build, testing and repository (Tlou)
- GitHub Actions now runs on every push to any branch and on every pull request. It runs the unit tests before building, and uploads the test report and the debug APK.
- Replaced the placeholder test with unit tests for the user record, sign-in errors, password rules, medication validation, the dose schedule, and phone numbers.
- `.gitignore` now excludes `google-services.json`, signing keys, Kotlin caches, and JVM crash dumps. The crash dumps that were committed by mistake have been removed.
- Added README sections on sign-in, Firebase setup, the CI workflow, and testing.

### Offline mode and sync (Lerato)
- **★ Offline mode with background sync** (Part 1 innovative feature 1). Medications, dose logs and health readings are saved on the phone first and flagged until they reach Firestore. WorkManager sends them as soon as the phone is back online, even if the app is closed.
- **Last write wins.** When the phone and Firestore disagree, the copy with the newer `updatedAt` is kept. Deletes made offline also reach Firestore and other phones.
- **Two-way sync.** Signing in on a new phone pulls that person's data down from Firestore. If a different account signs in, the previous account's local data is cleared so it is never uploaded to the wrong account.
- **Offline and syncing banner** on Home. It shows when the phone is offline, how many changes are waiting, and when a sync is running; tapping it syncs straight away.
- **★ Innovative feature 4: Refill calculator** (Part 1 innovative feature 5). Marking a dose Taken lowers the stock, and the pill cabinet shows the refill date, in red once a week or less is left. A refill reminder notification is shown once when the supply drops to seven days, and each medication can hold drug interaction notes.
- Room database moved to version 5 with a migration that keeps existing data. Firestore security rules were added in `firestore.rules`.
- Unit tests for the sync decisions, the Firestore mapping, the banner states and the refill calculator.

### Push notifications (Angel)
- _To be filled in by Angel._
- **★ Innovative feature 5:** _name and summary._

### Languages, interface and images (Ayanda)
- _To be filled in by Ayanda._

### Device testing and Play Store (Hlompho)
- _To be filled in by Hlompho._

## Part 2 prototype (baseline)

Medication cabinet with reminders (Taken / Snooze), 7-day, 30-day and custom history with a calendar and PDF export, health readings (weight, blood pressure, blood sugar) with charts, a caregiver missed-dose alert, English, isiZulu and Afrikaans, and email/password login with encrypted storage.
