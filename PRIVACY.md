# Cesto Privacy Policy

_Last updated: 11 October 2026_

Cesto is a personal app that reads your Continente receipt emails from Gmail, keeps the receipts on your
phone and lets you ask questions about your spending. This policy explains what data Cesto uses and where it goes.

## Data Cesto uses

| Data | Why | Where it is stored |
|---|---|---|
| Your Google account (name, email, account id) | Sign in, so the app knows which account is active | On your phone, and in Cesto's Google Identity Platform project (identity only) |
| Gmail: Continente receipt emails (`noreply@cartaocontinente.pt`) and their PDF attachments | Find and save your receipts | Only on your phone |
| Receipt contents (shop, date, items, prices, totals) | Show your receipts and answer your questions | Only on your phone (local database and PDF files) |
| Your Gemini API key (optional, entered by you) | Call Google Gemini with your own key | Only on your phone, encrypted with the Android Keystore |

Cesto has **no backend of its own**, no analytics, no ads and no tracking.

## Gmail access

- Cesto asks for **read-only** Gmail access (`gmail.readonly`).
- It only searches for Continente receipt emails. It never sends, deletes or changes emails.
- The Gmail access token stays on your phone and is only sent to Google's Gmail API.
- Emails and receipts are never sent to the developer or any server owned by the developer.

## Google Gemini

If you add your own Gemini API key, Cesto sends these to Google Gemini with **your** key:

- the text of a receipt, to read its items and prices, and
- your questions and small results from your local receipts (for example a total or a list of items),
  to answer them.

Cesto never sends the whole database or the raw emails to Gemini. Google processes this data under the
[Gemini API terms](https://ai.google.dev/gemini-api/terms) and [Google's Privacy Policy](https://policies.google.com/privacy).
Without a key, nothing is sent to Gemini.

## Limited Use

Cesto's use and transfer of information received from Google APIs to any other app will adhere to the
[Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy),
including the Limited Use requirements. Gmail data is used only to show you your own receipts and spending,
is not used for advertising, is not sold, and is not read by any person.

## Sharing

Cesto does not sell or share your data. Data only goes to Google services you use through the app
(Sign in with Google, Gmail API, and Gemini with your own key).

## Notifications

Cesto can show a daily spending notification. It is created on the phone from your local data.

## Deleting your data

- Uninstall the app, or clear its storage (Android Settings → Apps → Cesto → Storage → Clear storage), to remove
  the receipts, PDFs, database and Gemini key from your phone.
- Remove Cesto's Gmail access at any time: [myaccount.google.com/permissions](https://myaccount.google.com/permissions).
- To delete your sign-in account record, open an issue at the link below and it will be deleted.

## Children

Cesto is not meant for children under 13.

## Changes

Changes to this policy are published on this page with a new date.

## Contact

[github.com/mbahmani90/Cesto/issues](https://github.com/mbahmani90/Cesto/issues)
