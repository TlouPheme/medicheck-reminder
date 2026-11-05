# Demo video script (Part 3)

**Target length:** about 10 minutes. **Record on:** a real Android phone, using screen recording plus a voice-over. **Also open:** the Firebase console on a laptop, for the backend shots.

Upload the video to YouTube as **Unlisted**, then add the link to the README.

## Before recording

- [ ] Install the **release** APK (`app-release.apk`) on the phone, or the debug build if the release SHA-1 isn't in Firebase yet. Google sign-in needs it.
- [ ] Sign out, so the video starts at the login screen.
- [ ] Turn on **Do Not Disturb exceptions** for MediCheck. The full-screen alarm must be able to wake the phone.
- [ ] In the Firebase console, open **Authentication → Users**, **Firestore → users**, and **Messaging** in separate tabs.
- [ ] Add one medication due about **12 minutes** after recording starts. It's for the alarm scene, so leave it unanswered.

## Script

| Time | On screen | Voice-over |
| --- | --- | --- |
| 0:00 | Title card: app icon and team names | "This is MediCheck Reminder, our OPSC6312 Part 3 app. It's running on a real Android phone, and we'll show every feature and the data it stores in Firebase." |
| 0:20 | Login screen: tap **Sign in with Google** and pick an account | "Users can sign in with Google through Firebase Authentication and Android Credential Manager, or with an email and password. Email passwords are also stored on the phone as a salted, encrypted hash, so login works offline." |
| 0:45 | Firebase console: **Authentication → Users** | "Here's the account in Firebase Authentication, with Google as the provider." |
| 1:00 | Firestore: `users/{uid}` | "Both sign-in methods share one user record in Firestore, under the same uid. It holds the email, the linked providers, and this phone's push token." |
| 1:20 | App: Settings → **Language → isiZulu**, then **Afrikaans**, then back to English | "The app is available in English, isiZulu and Afrikaans. Changing the language updates every screen at once." |
| 1:45 | Medications → **+**: add *Metformin 500 mg*, daily at 08:00 and 20:00, stock **16**, interaction note *Avoid alcohol* | "Adding a medication. Wrong input gets a clear error. We set the stock and a drug interaction note from the pharmacist." |
| 2:15 | Pill cabinet card | "This is our first new feature, the refill calculator. From the stock and the schedule, it shows the date this medication runs out." |
| 2:30 | Home: tap **Taken** on Metformin, then back to Medications | "Marking a dose Taken lowers the stock. We're now at a week left, so the card turns red…" |
| 2:45 | Pull down the notification shade | "…and the app sends one refill reminder." |
| 3:00 | Turn on **airplane mode**. Home shows the offline banner | "Next, offline mode. With no connection, the banner says changes are saved on the phone." |
| 3:15 | Add a health reading (weight) and a medication *Vitamin D* | "Everything still works offline, and the banner counts the changes waiting to sync." |
| 3:35 | Turn **airplane mode off**. The banner shows *Syncing…* and disappears | "When the connection returns, WorkManager syncs the waiting changes to Firestore automatically. If the same record was changed elsewhere, the newest change wins." |
| 3:55 | Firestore: `users/{uid}/medications` and `health` | "Here's Vitamin D and the weight reading in Firestore. The app writes to the database and reads from it too: signing in on a new phone pulls this data back down." |
| 4:20 | History: 7 days, 30 days, and a custom range; the calendar | "History shows taken and missed doses for any period…" |
| 4:40 | History → **Export PDF** → share sheet | "…and exports a PDF report for the doctor." |
| 5:00 | Health: weight, blood pressure, blood sugar charts | "Health readings are logged and shown on 30-day charts." |
| 5:20 | Settings: caregiver name, phone and email; **Escalate missed doses** | "A caregiver can be told about a missed dose with one tap, by SMS or email." |
| 5:40 | Firebase console → **Messaging → Send test message** to the token from Firestore, with custom data `screen = medications`. The phone is on Home | "Push notifications use Firebase Cloud Messaging. Sending from an app event needs Cloud Functions, which require Firebase's paid plan, so we send from the console, as the brief allows." |
| 6:05 | The notification arrives; tap it and Medications opens | "Tapping the notification opens the screen it refers to." |
| 6:20 | Settings → **Push notifications** off, then show the token gone in Firestore | "Push notifications can be turned off in Settings. This also removes the phone's token." |
| 6:40 | Lock the phone and wait for the medication added before recording (cut the wait) | "Our second new feature: if a dose reminder isn't answered within 10 minutes…" |
| 6:50 | Full-screen alarm wakes the phone; the volume rises; tap **Taken** | "…it becomes a full-screen alarm over the lock screen, getting louder until it's answered with Taken, Snooze or Skip." |
| 7:20 | GitHub: Actions tab showing a green run, then the tests list | "Every push and pull request is built and unit-tested by GitHub Actions. We have 66 unit tests covering the main logic." |
| 7:45 | GitHub: pull requests list, then the release tag | "Each feature was built on its own branch and merged through a pull request. The final version is tagged v1.0-final-poe." |
| 8:05 | The `app-release.aab` file and the store listing document | "For the Play Store, we built a signed release bundle with its own upload key, kept out of GitHub, and prepared the store listing, graphics and privacy policy." |
| 8:30 | End card | "Thanks for watching." |

## After recording

- [ ] Upload to YouTube as **Unlisted** and copy the link.
- [ ] Add the link to the README, replacing the Part 2 video link.
- [ ] Check the voice-over can be heard over the alarm sound.
