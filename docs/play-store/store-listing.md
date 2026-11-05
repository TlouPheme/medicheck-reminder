# Google Play store listing

Prepared for Google Play internal testing. The group has no Play Console developer account, so the app has not been uploaded. Everything below is ready to paste in once an account exists.

## App details

| Field | Value |
| --- | --- |
| App name (30 max) | MediCheck Reminder |
| Default language | English (South Africa), en-ZA |
| App or game | App |
| Category | Medical |
| Free or paid | Free |
| Contact email | _the group's shared address_ |
| Privacy policy | <https://github.com/TlouPheme/medicheck-reminder/blob/main/docs/privacy-policy.md> |

### Short description (80 max)

> Medication reminders, refill alerts and health logs that work offline.

### Full description (4,000 max)

> MediCheck Reminder helps you take every dose on time, even without a connection.
>
> **Reminders that are hard to miss.** Add each medication with its dose and schedule: daily, on chosen days, every few hours, or as needed. A reminder arrives at each dose time with Taken and Snooze buttons. If you don't answer within 10 minutes, it becomes a full-screen alarm that gets louder until you respond.
>
> **Know when to refill.** Each dose you mark as taken comes off your stock. The pill cabinet shows the date you'll run out, turns red when a week is left, and sends one refill reminder. Keep your pharmacist's interaction warnings, such as "avoid grapefruit", on each medication.
>
> **Works offline.** Everything is saved on your phone first. When you're back online, your data syncs to your account automatically, so it's there on a new phone too.
>
> **History and reports.** See taken and missed doses for the last 7 or 30 days, or any range. Export a PDF report to share with your doctor.
>
> **Health log.** Record weight, blood pressure and blood sugar, and see 30-day charts.
>
> **Caregiver support.** Add a caregiver, and when a dose is missed, send them a message or email in one tap.
>
> **Made for South Africa.** Available in English, isiZulu and Afrikaans. Sign in with email or with Google.
>
> MediCheck Reminder is a reminder tool and does not give medical advice. Always follow your doctor's or pharmacist's instructions.

## Graphics

| Asset | Requirement | File |
| --- | --- | --- |
| App icon | 512 × 512 PNG, up to 1 MB | [`docs/play-store-icon-512.png`](../play-store-icon-512.png) (512 × 512, 10 KB) |
| Feature graphic | 1024 × 500 PNG or JPEG, no transparency | [`docs/feature-graphic-1024x500.png`](../feature-graphic-1024x500.png) (1024 × 500, 29 KB) |
| Phone screenshots | 2–8, 16:9 or 9:16, 320–3840 px per side | [`docs/play-store/screenshots/`](screenshots/) |

## Release

| Item | Value |
| --- | --- |
| Version code / name | 3 / 3.0.0 |
| Format | Signed Android App Bundle, `app-release.aab` (`./gradlew :app:bundleRelease`) |
| Signing | Upload key `medicheck-upload` (RSA 2048, valid 10,000 days) in a PKCS12 keystore kept **outside** the repository. Its passwords are in `keystore.properties`, which `.gitignore` excludes. Back both up: a lost upload key has to be reset through Play support. |
| Signed APK for sideloading | `app-release.apk` (`./gradlew :app:assembleRelease`) |

## Data safety form (answers)

- **Collected:** email address and name (account management), health info: medications, doses and readings (app functionality), photos (optional profile photo), device ID: the push token (app functionality).
- **Shared with third parties:** no.
- **Encrypted in transit:** yes (Firebase uses HTTPS).
- **Users can request deletion:** yes, as described in the privacy policy.
- **Ads:** no.

## Content rating

Medical reference and reminder app, with no user-to-user content, ads, or purchases. It should rate *Everyone* / PEGI 3.

## Before a real upload

- **Package name.** Google Play does not accept package names that start with `com.example`. Before a real upload, change `applicationId` (for example to `za.medicheck.reminder`) and register the new package in Firebase.
- **Release SHA-1.** Add the upload key's SHA-1 to Firebase so that Google sign-in works in release builds. Get it with:
  `keytool -list -v -keystore <path to medicheck-upload.jks> -alias medicheck-upload`
