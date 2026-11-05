# MediCheck Reminder: Privacy Policy

_Last updated: 5 November 2026_

MediCheck Reminder is a medication reminder app made by students for the OPSC6312 module. This policy explains what the app stores, where, and why.

## What the app stores

| Data | Where it is kept | Why |
| --- | --- | --- |
| Email address, name, and a salted password hash | On your phone, in encrypted storage | To sign you in, including without a connection |
| Your account (email, name, sign-in method: email/password or Google) | Firebase Authentication (Google) | To sign you in on any phone |
| Medications, dose history, stock levels, interaction notes, and health readings (weight, blood pressure, blood sugar) | On your phone (Room database), with a copy in Cloud Firestore under your account | To remind you, show your history, and keep your data when you change phones |
| Your user record: email, name, sign-in methods, last sign-in time, and the phone's push token | Cloud Firestore | To link your sign-in methods to one record and to send notifications to your phone |
| Profile photo, if you add one | Firebase Storage | To show your photo in Settings |
| Caregiver name, phone number, and email, if you enter them | On your phone only | To prepare a message or email to your caregiver when you choose to send one |

The app does **not** show advertising, does **not** sell or share your data with anyone for marketing, and does **not** use analytics or tracking.

## Who can see your data

Your data in Firebase is stored under your account, and the database rules allow only you to read or write it. Google operates Firebase and processes the data on the project's behalf. See [Firebase's privacy information](https://firebase.google.com/support/privacy). Messages to your caregiver are sent from your own Messages or email app, and only when you tap the button.

## Permissions

- **Notifications**: to show dose reminders, refill reminders, and alerts.
- **Alarms and reminders / full-screen alarm**: so a missed dose can wake the screen.
- **Internet and network state**: to sync your data and to know when you are offline.
- **Photos (picker)**: only the photo you choose for your profile.

## Keeping and deleting your data

Data stays until you delete it. Deleting a medication in the app also deletes it from Firestore. To have your account and all synced data deleted, open an issue at <https://github.com/TlouPheme/medicheck-reminder/issues> asking for deletion, without including personal health details. The team will remove the account and its Firestore and Storage data.

## Children

The app is not aimed at children under 13.

## Changes

Changes to this policy are made in this file. The date at the top shows the latest version.
