# MediCheck Reminder

MediCheck Reminder is an Android app that helps a person take medication on schedule. They add each medication, receive a reminder at the scheduled time, and mark the dose taken, skipped, or snoozed. History shows those outcomes for the last 7 days, the last 30 days, or a custom range. The same app records weight, blood pressure, and blood sugar. The interface is available in English, isiZulu, and Afrikaans.

A walkthrough of the app is in this demonstration: [MediCheck Reminder demo](https://youtu.be/Chz8wPsceyg).

## Students

- ST10177726 - Tlou Pheme
- ST10286000 - Ayanda Rikhotso
- ST10384687 - Angel Kgafela
- ST10451890 - Lerato Mojalefa
- ST10457083 - Hlompho Petja

## Screenshots

![Home, with the greeting, daily adherence, and today's doses](docs/home.jpg)![Medication cabinet](docs/medications.jpg)![History calendar for taken and missed doses](docs/history.jpg)![Settings for language, reminders, and the caregiver](docs/settings.jpg)

![Health screen showing a weight reading](docs/health-weight.jpg)![Health screen showing a blood pressure reading](docs/health-blood-pressure.jpg)![Health screen showing a blood sugar reading](docs/health-blood-sugar.jpg)

## Design considerations

Room stores medications, dose logs, and health readings on the phone. The account, an email and a salted password hash, is kept in encrypted preferences, so email login still works without a connection. Reminders are scheduled on the device, so a dose alert does not depend on a network connection. A cloud copy is only worth adding later if the record must move to another phone, or if a caregiver should be notified without anyone opening Messages or email.

History begins on the day a medication is added. Earlier days are left empty, because the person was not using the app yet and those doses were not missed.

Reminders use `AlarmManager`. The notification offers Taken and Snooze. Snooze uses the duration chosen in Settings. When missed-dose escalation is on, a follow-up notification can open a message or an email to the caregiver saved in Settings. Alarms are rebuilt when medications or reminder settings change, and again after the phone restarts. On Android 12 and later, exact delivery needs the system Alarms and reminders permission. Without it, the alert can arrive a little later.

The interface follows Material 3, with Plus Jakarta Sans, and draws edge to edge so text stays clear of the camera cutout and the system navigation bar. Icons are Uicons by Flaticon, credited on the Settings screen. Each medication keeps one pill color derived from its name, so the cabinet stays recognizable after the name is saved.

Language is applied with per-app locales (`en`, `zu`, and `af`). Screen titles, buttons, errors, and health labels come from string resources, so changing the language replaces the interface rather than a few words.

History can export the doses in the selected period as a PDF and hand that file to the system share sheet.

## Sign-in

A person can register with an email and password, or tap **Sign in with Google**. Google sign-in uses Android Credential Manager to choose the account, then exchanges the Google ID token for a Firebase Authentication session.

Both methods resolve to one user record. An email sign-up creates a Firebase Auth account with the same email and password, and Google sign-in uses Firebase's default "one account per email address" rule. A Google account and a password account with the same email therefore share one Firebase `uid`. The profile is written to Firestore at `users/{uid}` with a merge (email, display name, linked providers, and the last sign-in time). The write is not awaited, so Firestore sends it when the phone is next online.

Sign-in failures are handled without a crash. A cancelled account picker simply returns to the login screen. No Google account on the phone, no network, and a timeout each show a message. The Google button is hidden when the build has no `google-services.json`, so a clone without Firebase still builds and offers email login.

### Firebase setup (once per team)

1. Create a Firebase project and add an Android app with package `com.example.medicheckreminder`.
2. Add each developer's debug SHA-1 (`./gradlew signingReport`) and the release key's SHA-1.
3. Enable **Authentication → Google** and **Authentication → Email/Password**, and create a Firestore database.
4. Download `google-services.json` into `app/`. It is listed in `.gitignore`.

## Offline mode and sync

Every change is saved to Room on the phone first, so the app works the same with no connection. Medications, dose logs and health readings each carry an `updatedAt` time and a `pendingSync` flag. A change made offline stays flagged until it reaches Firestore.

Each change queues a WorkManager job with a network constraint. The job waits until the phone is online, even if the app has been closed, and then runs `FirestoreSyncEngine`. That engine compares every record on the phone with Firestore at `users/{uid}/medications`, `users/{uid}/doseLogs` and `users/{uid}/health`. The conflict rule is **last write wins**: the copy with the newer `updatedAt` is kept, whichever side it is on. Deletes are sent as a `deleted` marker, so a medication removed on one phone is removed on the others. The app reads from Firestore as well as writing to it. Signing in on a new phone pulls the person's data down.

The phone holds one person's data. If a different account signs in, the previous account's local data is cleared before anything is uploaded. A banner on Home shows whether the phone is offline, has changes waiting, or is syncing. Tap it to sync straight away.

The Firestore rules are in [`firestore.rules`](firestore.rules): each user can read and write only `users/{their uid}`.

### Demo steps (offline sync)

1. Sign in, open **Home**, and show that no banner is visible.
2. Turn on **airplane mode**. The banner says *You're offline. Changes are saved on this phone.*
3. Add a medication. The banner now counts *1 change will sync when you reconnect*.
4. Turn airplane mode **off**. The banner shows *Syncing your data…* and then disappears.
5. In the Firebase console, open **Firestore → users → (your uid) → medications** and show the new document. Its `updatedAt` matches the time it was added offline.

## Refill calculator (user-defined feature 4)

Marking a dose **Taken** takes one from that medication's stock, and changing it back puts one back. From the stock and the schedule, the app works out how many days are left and the date a refill is needed. The pill cabinet card shows *Refill by …*, or turns red once a week or less is left. When the supply first drops to seven days, a **refill reminder** notification is shown once. Each medication can also hold **drug interaction notes** from a pharmacist (for example "avoid grapefruit"), which show on its card. The calculation is in `RefillCalculator` and is covered by unit tests.

## GitHub

Source is hosted at [github.com/TlouPheme/medicheck-reminder](https://github.com/TlouPheme/medicheck-reminder). `main` is the branch that holds the shared history. Each feature is built on its own branch (for example `feature/sso` or `feature/offline-sync`) and merged through a pull request that another member reviews. A pull request is not merged while the GitHub Actions build is red. The final submitted commit is tagged `v1.0-final-poe`.

## GitHub Actions

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs on every push, on any branch, and on every pull request. The job:

1. checks out the repository and sets up Temurin JDK 21 and Gradle caching;
2. accepts the Android SDK licenses and installs the API 37 platform;
3. runs the unit tests with `./gradlew :app:testDebugUnitTest`;
4. builds the debug app with `./gradlew :app:assembleDebug`;
5. uploads the test report and the debug APK as build artifacts.

A green run means the app compiles on a clean machine and every unit test passes. The result is shown on the commit and on the pull request. The runner has no `google-services.json`, so it builds the app with Google sign-in switched off.

## Testing

Unit tests live in [`app/src/test`](app/src/test) and run on the JVM:

| Test | Covers |
| --- | --- |
| `UserProfileTest` | The Firestore user record shared by email and Google sign-ins |
| `GoogleSignInErrorsTest` | Cancelled, no-account, and no-network sign-in outcomes |
| `PasswordValidatorTest` | Password complexity and the strength meter |
| `MedicationValidatorTest` | Add/edit medication rules and days of stock remaining |
| `DoseScheduleTest` | Daily, specific-day and interval schedules, missed doses |
| `SouthAfricanPhoneValidatorTest` | Caregiver phone numbers |
| `SyncResolverTest` | Last-write-wins decisions, offline edits and deletes |
| `SyncMappersTest` | Room rows to Firestore documents and back |
| `SyncStatusRulesTest` | The offline / syncing banner |
| `RefillCalculatorTest` | Days remaining, refill date, low-stock reminder, stock changes |
| `ImageScalingTest` | Shrinking profile photos before upload |

Run them locally with:

```bash
./gradlew :app:testDebugUnitTest
```
