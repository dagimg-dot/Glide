Great choice. Scoping **v1 to Clipboard only** is exactly how Samsung-level features start without burning time.

Below is a **clean, comprehensive, implementation-ready PRD** for **Edge Panel – Clipboard Manager (v1)**, tailored for **Android / Kotlin / Nothing OS–like AOSP devices**.

---

# 📄 Product Requirements Document (PRD)

## Edge Panel – Clipboard Manager (v1)

### Version

* **v1.0 (MVP)**

### Platform

* Android (Nothing Phone 3a)
* Kotlin
* Minimum SDK: **26 (Android 8.0)**
* Target SDK: Latest stable

---

## 1️⃣ Objective

Build a **system-wide Edge Panel Clipboard Manager** that:

* Is accessible from any app via **edge swipe**
* Displays **clipboard history**
* Allows **quick copy & reuse**
* Is **fast, minimal, and battery-safe**

---

## 2️⃣ In-Scope (v1)

### Core

✅ Clipboard history capture
✅ Edge swipe panel (overlay)
✅ List clipboard items
✅ Tap to re-copy
✅ Clear individual / all items

### UX

✅ Smooth open/close animation
✅ Minimal UI (no clutter)
✅ Works across all apps

---

## 3️⃣ Out of Scope (Future Versions)

❌ Multiple panels
❌ Cross-device sync
❌ Pin / favorite clips
❌ Rich preview (images, formatting)
❌ Search / tagging
❌ Cloud backup

---

## 4️⃣ User Stories

### US-1: View clipboard history

> As a user, I want to see previously copied text so I can reuse it easily.

### US-2: Re-copy clipboard item

> As a user, I want to tap a clipboard item to copy it again.

### US-3: Access from anywhere

> As a user, I want to open the clipboard panel without leaving my current app.

### US-4: Manage clipboard items

> As a user, I want to delete individual items or clear all clipboard history.

---

## 5️⃣ Functional Requirements

### 5.1 Clipboard Capture

* Monitor clipboard changes using `ClipboardManager`
* Capture **plain text only**
* Ignore:

  * Empty clips
  * Duplicates (configurable: last item only)
* Timestamp every entry

#### Data Fields

| Field                 | Type   |
| --------------------- | ------ |
| id                    | UUID   |
| text                  | String |
| timestamp             | Long   |
| source_app (optional) | String |

---

### 5.2 Clipboard Storage

* In-memory cache + local persistence
* Storage options:

  * `Room` (recommended)
* Max entries:

  * **Default: 50**
  * FIFO eviction

---

### 5.3 Edge Panel Trigger

* Visible **edge handle**
* Configurable:

  * Position (left / right)
  * Height
* Gesture:

  * Swipe inward → open panel
  * Tap outside → close panel

---

### 5.4 Overlay Panel

* Uses `TYPE_APPLICATION_OVERLAY`
* Width: ~80% screen
* Height: full screen
* Z-order above apps

---

### 5.5 Clipboard List UI

* Vertical scroll list
* Each item shows:

  * Truncated text (2–3 lines)
  * Timestamp (relative: “2m ago”)
* Actions:

  * Tap → copy to clipboard
  * Long press → delete

---

### 5.6 Panel Behavior

* Auto-close on:

  * Copy action
  * Outside touch
  * Back gesture
* Open/close animation ≤ 250ms

---

## 6️⃣ Non-Functional Requirements

### Performance

* Clipboard listener must not block UI
* Overlay render time < 100ms

### Battery

* Foreground service only when enabled
* No wake locks
* Respect Doze mode

### Security & Privacy

* Clipboard data:

  * Stored locally only
  * Never transmitted
* Clear disclaimer on first launch

---

## 7️⃣ Permissions

### Required

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### Optional (UX improvement)

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## 8️⃣ App Lifecycle & Services

### Clipboard Service

* Type: Foreground Service
* Responsibilities:

  * Clipboard listening
  * Persistence
  * Panel trigger coordination

### Overlay Lifecycle

* Created on service start
* Destroyed on service stop

---

## 9️⃣ Architecture

### High-Level Diagram

```
[ClipboardManager]
        ↓
[ClipboardService]
        ↓
[Room DB] ←→ [In-memory Cache]
        ↓
[Edge Overlay UI]
```

---

## 🔧 Suggested Package Structure

```
com.edgepanel.clipboard
│
├── service/
│   └── ClipboardService.kt
│
├── overlay/
│   ├── EdgeHandleView.kt
│   ├── ClipboardPanelView.kt
│
├── data/
│   ├── ClipboardEntity.kt
│   ├── ClipboardDao.kt
│   └── ClipboardDatabase.kt
│
├── ui/
│   └── ClipboardItemComposable.kt
│
└── util/
    └── TimeFormatter.kt
```

---

## 🔟 UX Guidelines

* Dark theme default
* One-hand friendly
* Haptic feedback on:

  * Panel open
  * Copy action
* No ads, no popups in v1

---

## 1️⃣1️⃣ Edge Cases

| Case               | Behavior              |
| ------------------ | --------------------- |
| App killed         | Service restarts      |
| Permission revoked | Disable panel         |
| Clipboard cleared  | Show empty state      |
| Duplicate copy     | Ignore or move to top |

---

## 1️⃣2️⃣ Success Metrics (v1)

* Panel open < 250ms
* Zero ANRs
* Clipboard capture accuracy > 99%
* No background crashes in 24h run

---

## 1️⃣3️⃣ Risks & Mitigations

| Risk                       | Mitigation           |
| -------------------------- | -------------------- |
| Android kills service      | Foreground service   |
| Overlay permission fear    | Clear onboarding     |
| Clipboard privacy concerns | Local-only guarantee |

---

## 1️⃣4️⃣ v1 Definition of Done

* Edge swipe works system-wide
* Clipboard history persists
* Copy action reliable
* Battery usage acceptable
* Tested on Nothing OS

---

## 1️⃣5️⃣ v2 Preview (Not Implemented)

* Multiple panels
* Pin clipboard items
* App-based grouping
* Image clipboard

---

### ✅ Final Verdict

This PRD is **tight, buildable in 1–2 weeks**, and sets a strong foundation for a Samsung-grade Edge Panel.

If you want next:

* I can convert this into **tasks / Jira tickets**
* Or write a **ClipboardService + Overlay skeleton**
* Or design **Compose UI for the panel**

Just tell me the next step 👌
