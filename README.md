[project_readme.md](https://github.com/user-attachments/files/34106769/project_readme.md)
# Aegis Habits 🛡️✨

> **Forge unbreakable consistency with local-first habit tracking, Glance widget synchronization, and the Aegis Shield system.**

---

## 🌟 Overview

**Aegis Habits** is an advanced, offline-first Android habit tracker built with **Jetpack Compose**, **Kotlin Coroutines / Flows**, **Room Database**, and **Jetpack Glance**. Designed for reliability and privacy, Aegis ensures your personal growth data never leaves your device while offering powerful mechanics like the **Aegis Streak Shield System** to protect against unexpected life disruptions.

---

## 🏛️ Architecture & Technical Deliverables

### 1. App Widget State Synchronization & WorkManager Lifecycle
Aegis features native home screen widgets built using **Jetpack Glance**, keeping your habits glanceable and interactive right from your launcher.
* **State Synchronization:** The `HabitViewModel` triggers targeted widget updates (`triggerWidgetUpdate(context)`) whenever habits are toggled, completed, or shielded. This ensures Glance components reflect real-time database state instantly without stale UI caches.
* **Background Processing:** **WorkManager** manages asynchronous background routines, periodic reminder scheduling, and notification dispatching respecting frequency-aware scheduling (daily, specific weekdays, custom intervals).

### 2. The Aegis Shield System (Anti-Streak Reset Mechanism)
Life happens. Missing a single day shouldn't destroy weeks of hard-earned momentum.
* **Bridge Gaps:** When a user misses a day and toggles completion on a subsequent day, the system evaluates the gap (`daysBetween > 1`).
* **Automated Consumption:** If `shieldsAvailable > 0`, a shield is automatically consumed to bridge the gap, preventing streak resets.
* **Persistent Integrity:** State updates are strictly synchronized across the `HabitRepository` and `HabitViewModel`, ensuring shields cannot be used infinitely and milestone rewards are properly applied *after* shield deductions.

### 3. Local Data Schema Documentation (Room Database)
Aegis operates 100% locally with zero cloud dependencies, ensuring complete user privacy.

* **`Habits` Table**:
  * `id` (Long, Primary Key, Auto-generate)
  * `title` (String)
  * `description` (String)
  * `frequency` (String / Enum)
  * `shieldsAvailable` (Int) — Tracks currently stored Aegis shields.
  * `createdAt` (Long)

* **`HabitCompletions` Table**:
  * `id` (Long, Primary Key, Auto-generate)
  * `habitId` (Long, Foreign Key referencing `Habits`)
  * `dateString` (String, formatted as `yyyy-MM-dd`)

---

## 🚀 Key Features

* **🛡️ Aegis Shield System**: Store and deploy shields to protect your streaks when life gets busy.
* **📊 Advanced Analytics**: Track completion rates, current streaks, best streaks, and deep-dive correlations using custom time ranges (`AnalyticsViewModel`).
* **⚡ Jetpack Glance Widgets**: Beautiful, lightweight, and reactive home screen widgets for instant habit completion toggling.
* **🔒 100% Local-First**: Powered by Room and Kotlin Flows. No external accounts, no cloud tracking, and works entirely offline.
* **⏰ Smart Reminders**: Frequency-aware scheduling engine ensuring you never miss a habit window.

---

## 🛠️ Tech Stack

* **UI**: Jetpack Compose, Material 3, Jetpack Glance (Widgets)
* **Architecture**: MVVM (Model-View-ViewModel), Unidirectional Data Flow (UDF) with Kotlin Flows & Coroutines
* **Database & Persistence**: Room (SQLite), KSP (Kotlin Symbol Processing)
* **Background Tasks**: Android WorkManager
* **Date & Time**: `java.time` (LocalDate API)

---

## 🧪 Setup & Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Aarush3K/AegisHabits.git
   ```
2. Open the project in **Android Studio** (Hedgehog or newer recommended).
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device (Min SDK 26+).

---

