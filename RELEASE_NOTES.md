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
- _To be filled in by Lerato._
- **★ Innovative feature 4:** _name and summary._

### Push notifications (Angel)
- _To be filled in by Angel._
- **★ Innovative feature 5:** _name and summary._

### Languages, interface and images (Ayanda)
- _To be filled in by Ayanda._

### Device testing and Play Store (Hlompho)
- _To be filled in by Hlompho._

## Part 2 prototype (baseline)

Medication cabinet with reminders (Taken / Snooze), 7-day, 30-day and custom history with a calendar and PDF export, health readings (weight, blood pressure, blood sugar) with charts, a caregiver missed-dose alert, English, isiZulu and Afrikaans, and email/password login with encrypted storage.
