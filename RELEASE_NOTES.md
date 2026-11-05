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
- **Firebase Cloud Messaging.** `MediCheckMessagingService` receives pushes and shows them on a new **Updates and alerts** channel.
- **Token on the user record.** Each phone's FCM token is saved to `users/{uid}.fcmTokens` after sign-in and updated when it changes. It is removed on sign-out.
- **Notification permission.** It is requested on Android 13 and later.
- **Tap opens the right screen.** A push or notification carrying `screen` opens that tab (home, medications, history, health or settings). The refill reminder opens Medications.
- **Settings → Push notifications** on/off. Turning it off removes the phone's token from Firestore and FCM.
- Pushes are sent from the Firebase console's test message. Sending from an app event needs Cloud Functions, which are not on the free plan.
- **★ Innovative feature 5: Escalating full-screen alarm** (Part 1 FR-INN-01). A dose reminder not answered within 10 minutes becomes a full-screen alarm over the lock screen. Its sound gets louder over time, it vibrates, and it offers Taken, Snooze and Skip. It can be switched off under **Settings → Full-screen alarm**.
- Unit tests for push routing and the alarm rule.

### Languages, interface and images (Ayanda)
- **Profile photo.** A profile row at the top of Settings opens the photo picker. The photo is shrunk on the phone, uploaded to Firebase Storage, and its URL is saved on `users/{uid}` in Firestore (blob storage and NoSQL learning outcomes). Progress, success and error messages are shown. A default avatar follows the light and dark theme.
- **Themed launcher icon.** A monochrome layer was added to the adaptive launcher icon for Android 13+ themed icons.
- **Store graphics.** 512 × 512 icon and 1024 × 500 feature graphic.
- `storage.rules`: each user can read and write only their own JPEG under 2 MB.
- Profile text in English, isiZulu and Afrikaans, and an `ImageScaling` helper with unit tests.
- Fixes during review: the upload uses the signed-in Firebase user instead of an anonymous account, the cached photo is cleared when the account changes, and the `<monochrome>` tag typo is corrected.

### Device testing and Play Store (Hlompho)
- **Device testing.** Every feature was tested on a physical OnePlus CPH2669 (Android 16), and the bugs found were logged as GitHub Issues #6–#9.
- **Fix.** Answered doses on Home now show their status, "✓ Taken" or "Skipped", instead of an empty row (#6).
- **Signed release.** Upload keystore created and kept out of Git. Signed release AAB and APK, version **3 / 3.0.0**.
- **Store listing.** Name, short and full description, category, data safety answers, content rating, and phone screenshots.
- **Privacy policy.** Added at `docs/privacy-policy.md`. The in-app Privacy link now opens it instead of a domain the team does not own.
- **Video script.** Demo video script covering every feature and the Firebase data.
- **AI-use write-up.** Added at `docs/ai-use-write-up.md`.

## Part 2 prototype (baseline)

Medication cabinet with reminders (Taken / Snooze), 7-day, 30-day and custom history with a calendar and PDF export, health readings (weight, blood pressure, blood sugar) with charts, a caregiver missed-dose alert, English, isiZulu and Afrikaans, and email/password login with encrypted storage.
