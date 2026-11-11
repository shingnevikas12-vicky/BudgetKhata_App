<<<<<<< HEAD
# Budget Khata – Student Contribution Manager

An offline-first Android app for student contribution accounting: events, per-student contributions,
partial payments, expenses, WhatsApp reminders, UPI links/QR codes, and CSV/PDF reports.

Built as a real, working app (not a UI prototype): every screen writes to a Room database, every
total is calculated from confirmed records, and every export produces a real file.

---

## 1. What it does

| Area | Behaviour |
|---|---|
| Dashboard | Student count, expected contribution, collected, pending, paid/partial/pending counts, collection donut, event bar chart, recent payments, search, quick actions |
| Students | Add / edit / delete / archive, search by name or mobile, filter by payment status and event; per-student profile with event enrollments and full payment ledger |
| Events | Jatra, NSS, festivals, tours, parties or custom; each event has its own student list, contributions, collections, expenses and net balance; archive instead of delete |
| Payments | Partial and multiple payments per enrollment; each entry stores amount, date, method, reference and notes. Remaining = required − confirmed paid. Status is always derived (Paid / Partial / Pending) |
| Reminders | Personalised WhatsApp message with student name, event, total, paid, pending, UPI ID, payee, payment link and instructions. Opens WhatsApp with the number pre-filled — **you** tap Send |
| UPI & QR | Permanent receive QR for the organiser account (default `9730914525@ptyes` · *Mr Vikas Devidas Shingne*), amount-specific `upi://pay` links, editable UPI ID/payee/instructions, optional own QR image, share/copy actions |
| Expenses | Description, amount, date, category, notes and an optional receipt image; total expenses and net balance per event |
| Reports | Paid students, partial list, pending students, student ledger, event-wise summary, expense report → preview + CSV export + paginated PDF + share |
| Data | JSON backup/restore, CSV/PDF exports, delete-all with typed confirmation, optional screenshot blocking |
| Themes | Material 3 light/dark/system, optional dynamic colour, semantic green (paid) / orange (partial) / red (pending) |

**No admin PIN:** the app opens directly on the dashboard. Back on the home screen sends it to the
background instead of closing it, so you never restart from scratch. The only privacy control left is
"screenshot blocking" in Settings → Privacy.

**Payment-verification policy:** the only code path that writes a payment is the administrator's
"Record a payment" sheet (with an explicit "I confirm this money was actually received" checkbox).
Opening a UPI app, scanning a QR, or sending a WhatsApp message never changes a payment record.
The UPI PIN is always entered inside the payment app — no app can store or use it.

---

## 2. Tech stack

- Kotlin 2.0.21, Jetpack Compose (BOM 2024.11.00, Material 3 1.3.1), Navigation Compose 2.8.4
- MVVM with manual dependency injection (`AppContainer`) — no Hilt, no reflection
- Room 2.6.1 via KSP (5 entities, foreign keys with cascade, aggregated SQL views)
- DataStore Preferences for settings, `EncryptedSharedPreferences` + PBKDF2 for the PIN
- ZXing `core` for QR generation, Gson for JSON backups, `PdfDocument` for PDFs
- Money is stored as **paise (Long)** everywhere; dates as epoch-day `Long`
- `minSdk 24`, `targetSdk/compileSdk 35`, JVM target 17
- No runtime permissions: exports use SAF, file sharing uses FileProvider, UPI/WhatsApp use implicit intents

---

## 3. Build and run

Requirements: Android Studio (Ladybug or newer), JDK 17+, Android SDK Platform 35.

```bash
# from the project root
./gradlew :app:assembleDebug          # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # 50 JVM tests
./gradlew :app:connectedDebugAndroidTest   # Room tests on a device/emulator
```

Install on a connected phone:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run. `local.properties` points at the SDK location
(`sdk.dir=...`); change it if your SDK lives elsewhere.

The Gradle wrapper is included (Gradle 8.9). The first build downloads the Android Gradle Plugin,
Compose, Room and ZXing if they are not already in the Gradle cache.

### First run

1. The dashboard opens immediately (no PIN).
2. Your permanent UPI account (`9730914525@ptyes` · *Mr Vikas Devidas Shingne*) is already configured —
   see Settings → UPI & payment QR.
3. Create an event (e.g. *College Jatra 2026*).
4. Add students, optionally enrolling each in an event with a contribution amount.
5. Record payments as you receive them. The dashboard updates immediately.

---

## 4. Project structure

```
app/src/main/java/com/budgetkhata/
├─ BudgetKhataApp.kt / MainActivity.kt        Application + edge-to-edge entry point
├─ di/AppContainer.kt                         Manual DI graph (database, repositories, integrations)
├─ core/                                      Money (paise), dates, phone normalisation, validation, status
├─ data/local/                                Room: entities, DAOs, relations, aggregate models, database
├─ data/repo/                                 Student, event, enrollment, payment, expense repositories
├─ data/prefs/                                AppSettings + DataStore, SecurityStore (PIN), LockController
├─ data/backup/                               JSON backup DTOs + transactional restore/validation
├─ domain/                                    Reminder text, UPI links, report tables, CSV, PDF writer
├─ integrations/                              WhatsApp, UPI launcher, QR generator, share/clipboard helpers
└─ ui/
   ├─ theme/                                  Material 3 colour schemes, typography, shapes
   ├─ components/                             Stat cards, chips, charts, inputs, empty/loading/error states
   ├─ navigation/                             Routes + NavHost
   ├─ lock/                                   PIN setup and lock screens
   ├─ dashboard/ students/ events/            Screens + ViewModels
   ├─ payments/                               Record-payment dialog, contribution dialog, reminder actions
   ├─ expenses/ reports/ settings/            Screens + ViewModels
```

### Data model

```
students (name, phone, altPhone, notes, archived)
events   (name, type, description, date, archived)
enrollments (studentId → students, eventId → events, requiredPaise, unique student+event)
payments (enrollmentId → enrollments, amountPaise, paidEpochDay, method, reference, notes, source)
expenses (eventId → events, title, amountPaise, spentEpochDay, category, receiptUri, notes)
```

A person is stored once and can join many events; the contribution belongs to the enrollment. Deleting a
student or event cascades to its enrollments, payments and expenses. Payment status, remaining amount,
event totals and net balance are computed from confirmed records on every read — never stored.

---

## 5. How the tricky parts work

**WhatsApp reminder.** `WhatsAppLauncher` normalises the number to international digits, checks for
WhatsApp/WhatsApp Business, and sends `whatsapp://send?phone=…&text=…` (falling back to `wa.me`). The
message is rendered from an editable template with placeholders `{name} {event} {total} {paid} {pending}
{upi_id} {payee} {upi_link} {instructions} {upi_block}`. If WhatsApp is missing, the app offers to copy
the message; an invalid number opens a dialog explaining what to fix. Nothing is sent automatically.

**UPI and QR.** A UPI link is `upi://pay?pa=…&pn=…&am=…&cu=INR&tn=…`; per-student links/QRs include the
exact pending amount. Every place that opens a UPI app says that opening it does not confirm payment.
For automatic verification you need a payment gateway with a server-side webhook — the schema already
separates `PaymentSource.MANUAL` from `GATEWAY` for that future integration.

**PDF reports.** `PdfReportWriter` lays out A4 pages with a coloured header, paginated table rows,
totals and a footer, writes to `cacheDir/reports/`, and shares through FileProvider.

**CSV exports.** RFC-4180 quoting with a UTF-8 BOM so Excel shows ₹ correctly. Exported through
`ACTION_CREATE_DOCUMENT`, so no storage permission is needed.

**Backups.** JSON containing students, events, enrollments, payments, expenses and settings (the PIN is
never exported). Restore validates the format, version and referential integrity, shows the row counts,
then replaces everything inside a single Room transaction.

**Security.** No login PIN is used. Optional `FLAG_SECURE` screenshot blocking is available in
Settings → Privacy, and `allowBackup=false` keeps the database out of cloud backups. Exports use SAF
and FileProvider, so the app still requests no runtime permissions.

---

## 6. Tests

- `app/src/test`: 50 JVM tests — money parsing/Indian formatting, payment status rules, phone
  normalisation, validation (including overpayment limits), reminder template rendering, UPI link
  encoding, CSV escaping, report tables, model aggregates, backup JSON round-trip.
- `app/src/androidTest`: `DatabaseTest` — Room schema, dashboard roll-ups from payments, cascade delete,
  event summary with expenses and net balance (run on a device).

---

## 7. Known limitations

- Automatic payment verification is intentionally not implemented (requires a gateway + server).
- UPI/QR/WhatsApp are sharing mechanisms only; the admin confirms every payment.
- The permanent QR is generated from your UPI ID; to show the exact Paytm poster image, use
  "Use own QR image" on the UPI screen and pick your saved poster.
- WhatsApp cannot target a specific number *and* attach an image in one intent, so the phone-targeted
  reminder carries the UPI text/link and QR images are shared separately.
- Receipt attachments use SAF URIs; the provider must support persistable grants (most do).
- The app has no cloud sync or multi-admin support by design; use backup/restore to move data.
=======
# BudgetKhata_App
BudgetKhata is an easy-to-use app that helps you keep track of your daily income and expenses, manage your money, and see where your money goes.
>>>>>>> 0402c501f8a782d0761f5904324b97fe74e8fffe
