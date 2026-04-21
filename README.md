# 🏋️ Elite Fitness Club — Booking System

> A command-line Java application for managing weekend group fitness class bookings, attendance tracking, and revenue reporting at Elite Fitness Club (EFC).

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Domain Model](#domain-model)
- [Business Rules](#business-rules)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Build](#build)
  - [Run](#run)
- [Usage Guide](#usage-guide)
- [Sample Data](#sample-data)
- [Reports](#reports)
- [Testing](#testing)
- [Technology Stack](#technology-stack)

---

## Overview

The **EFC Booking System** is a fully in-memory, menu-driven Java application that manages weekend group fitness classes for a gym. It handles the complete booking lifecycle — from placing a reservation, modifying or cancelling it, recording attendance with feedback, and generating attendance and revenue reports.

The system covers **8 weekends** of programming, with **6 sessions per weekend** across Saturday and Sunday, accommodating up to **4 members per class**.

---

## Features

| Feature | Description |
|---|---|
| 📅 **View Schedule** | Browse sessions by day (Sat/Sun) or by fitness type |
| ✅ **Place Booking** | Book a class with full constraint validation |
| ✏️ **Modify Booking** | Reassign an existing booking to a different session |
| ❌ **Cancel Booking** | Cancel a booking and free up the spot |
| 🎯 **Record Attendance** | Mark attendance and submit a 1–5 star rating with comment |
| 📖 **View My Bookings** | List all bookings (active and historical) for a member |
| 👥 **View All Members** | Display the full member directory |
| 📊 **Attendance Report** | Session-level head count and average feedback ratings |
| 💰 **Revenue Report** | Income breakdown by fitness type, with top-earner highlighted |

---

## Project Structure

```
EFC/
├── pom.xml                                          # Maven build file
└── src/
    ├── main/
    │   └── java/efc/
    │       ├── Main.java                            # Application entry point
    │       ├── model/
    │       │   ├── Member.java                      # Gym member entity
    │       │   ├── ClassSession.java                # Individual class session
    │       │   ├── Booking.java                     # Booking record (member ↔ session)
    │       │   ├── Feedback.java                    # Post-attendance rating & comment
    │       │   ├── Schedule.java                    # In-memory session repository
    │       │   ├── BookingStatus.java               # Enum: BOOKED | MODIFIED | CANCELLED | ATTENDED
    │       │   ├── Day.java                         # Enum: SATURDAY | SUNDAY
    │       │   └── TimeSlot.java                    # Enum: MORNING | MIDDAY | EVENING
    │       ├── service/
    │       │   └── BookingManager.java              # Central business logic façade
    │       ├── data/
    │       │   └── DataSeeder.java                  # Seeds members, sessions, and feedback
    │       └── ui/
    │           └── FitnessUI.java                   # Interactive CLI menu
    └── test/
        └── java/efc/
            └── EFCSystemTest.java                   # JUnit 5 test suite
```

---

## Architecture

The application follows a clean **layered architecture** with a central **Façade pattern**:

```
┌─────────────────────────────────────────┐
│              FitnessUI (CLI)            │  ← User interaction layer
│         Reads input, prints output      │
└────────────────────┬────────────────────┘
                     │ delegates all logic
┌────────────────────▼────────────────────┐
│           BookingManager (Façade)       │  ← Business logic layer
│   Enforces all rules & state changes    │
└──────┬───────────────────────┬──────────┘
       │                       │
┌──────▼──────┐       ┌────────▼────────┐
│  Schedule   │       │  Member Store   │  ← Domain model layer
│ (Sessions)  │       │  (Members Map)  │
└──────┬──────┘       └─────────────────┘
       │
┌──────▼──────────────────────────────────┐
│   ClassSession → Booking → Feedback     │  ← Entity layer
└─────────────────────────────────────────┘
```

**Key design decisions:**
- `BookingManager` is the single entry point for all operations — the UI never touches domain objects directly.
- All data is stored **in-memory** using `LinkedHashMap` for insertion-order preservation.
- Booking references (e.g. `EFC-0001`) are **never reused** after cancellation.
- `Schedule` acts as a **repository** providing multi-key query capability (by ID, day, type, and slot).

---

## Domain Model

### `Member`
Represents a registered gym member.

| Field | Type | Description |
|---|---|---|
| `memberId` | `String` | Unique ID (e.g. `EFC-M01`) |
| `fullName` | `String` | Full display name |
| `phone` | `String` | Contact phone number |
| `email` | `String` | Email address |

---

### `ClassSession`
A single scheduled group fitness class.

| Field | Type | Description |
|---|---|---|
| `sessionId` | `String` | Unique ID (e.g. `W1SAM`) |
| `fitnessType` | `String` | Class type (Pilates, HIIT, etc.) |
| `day` | `Day` | `SATURDAY` or `SUNDAY` |
| `timeSlot` | `TimeSlot` | `MORNING`, `MIDDAY`, or `EVENING` |
| `weekNumber` | `int` | Week 1–8 |
| `fee` | `double` | Price per participant (£) |
| `enrolledIds` | `List<String>` | Active enrolled member IDs (max 4) |
| `feedbackList` | `List<Feedback>` | All submitted feedback entries |

---

### `Booking`
Links a `Member` to a `ClassSession`.

| Field | Type | Description |
|---|---|---|
| `bookingRef` | `String` | Unique reference (e.g. `EFC-0001`) |
| `memberId` | `String` | The member who booked |
| `sessionId` | `String` | The session booked |
| `status` | `BookingStatus` | Current lifecycle state |

**Booking Lifecycle:**

```
  placeBooking()       modifyBooking()
       │                     │
  ┌────▼────┐           ┌────▼────┐
  │ BOOKED  ├──────────►│MODIFIED │
  └────┬────┘           └────┬────┘
       │                     │
       │   cancelBooking()   │
       └──────────┬──────────┘
                  ▼
            ┌──────────┐
            │CANCELLED │  (permanent — ref retired)
            └──────────┘
       ┌────┬────┐
       │ BOOKED  │  recordAttendance()
       │MODIFIED ├─────────────────────►  ATTENDED
       └─────────┘
```

---

### `Feedback`
Immutable post-attendance value object.

| Field | Type | Constraints |
|---|---|---|
| `memberId` | `String` | — |
| `sessionId` | `String` | — |
| `rating` | `int` | 1 (Very Poor) → 5 (Excellent) |
| `comment` | `String` | Free text |

---

### Enums

**`Day`**
| Value | Label |
|---|---|
| `SATURDAY` | Saturday |
| `SUNDAY` | Sunday |

**`TimeSlot`**
| Value | Label | Window |
|---|---|---|
| `MORNING` | Morning | 07:00 – 08:30 |
| `MIDDAY` | Midday | 12:00 – 13:30 |
| `EVENING` | Evening | 18:00 – 19:30 |

**`BookingStatus`**
`BOOKED` → `MODIFIED` → `CANCELLED` / `ATTENDED`

---

## Business Rules

All rules are enforced inside `BookingManager`:

1. **Capacity** — Each session holds a maximum of **4 participants**. Attempts to book a full session are rejected.
2. **No duplicates** — A member cannot hold two active bookings for the same session.
3. **Time-slot conflict** — A member cannot be booked into two sessions that share the same `week`, `day`, and `timeSlot`.
4. **Unique booking references** — References follow the pattern `EFC-XXXX` and are never recycled after cancellation.
5. **Active-only mutations** — Only bookings with status `BOOKED` or `MODIFIED` can be modified or cancelled.
6. **Attendance requires active booking** — `recordAttendance()` only works on active bookings.
7. **Valid feedback rating** — Ratings outside the 1–5 range throw `IllegalArgumentException`.
8. **Revenue counting** — Only `ATTENDED` bookings contribute to revenue reports.

---

## Getting Started

### Prerequisites

- **Java 17** or higher
- **Apache Maven 3.6+**

Verify your setup:
```bash
java -version
mvn -version
```

### Build

Clone the repository and build the project:

```bash
git clone https://github.com/<your-username>/EFC-Booking-System.git
cd EFC-Booking-System

# Compile and run tests
mvn clean package
```

This produces a fat JAR at:
```
target/EFC-Booking-System.jar
```

### Run

```bash
java -jar target/EFC-Booking-System.jar
```

You will be greeted by the EFC welcome banner and the main menu.

---

## Usage Guide

On launch, the system seeds sample data automatically and presents the following menu:

```
  ╔══════════════════════════════════════════════════╗
  ║     ELITE FITNESS CLUB — BOOKING SYSTEM  v2.0   ║
  ╚══════════════════════════════════════════════════╝

  1. View Class Schedule
  2. Book a Class
  3. Modify a Booking
  4. Cancel a Booking
  5. Attend a Class & Submit Feedback
  6. View My Bookings
  7. View All Members
  8. Attendance Report
  9. Revenue Report
  0. Exit
```

### Option 1 — View Schedule

Browse available sessions filtered by:
- **Day** — Saturday or Sunday
- **Fitness Type** — Pilates, HIIT, CrossFit, Strength Training, Yoga Flow

### Option 2 — Book a Class

1. Enter your **Member ID** (e.g. `EFC-M01`)
2. View the list of available sessions
3. Enter the **Session ID** (e.g. `W3SAD`)
4. A booking reference like `EFC-0023` is returned on success

### Option 3 — Modify a Booking

1. Enter your **Member ID**
2. Your active bookings are listed
3. Enter the **Booking Reference** to modify
4. Enter the **new Session ID**
5. The system validates availability and conflict before reassigning

### Option 4 — Cancel a Booking

1. Enter your **Member ID**
2. Select the **Booking Reference** to cancel
3. The spot is freed; the booking record is retained with `CANCELLED` status

### Option 5 — Attend a Class & Submit Feedback

1. Enter your **Member ID**
2. Select the **Booking Reference** for the attended class
3. Submit a **rating (1–5)** and a **comment**
4. Booking status changes to `ATTENDED`

### Option 6 — View My Bookings

Displays all bookings (active and historical) for the given member ID, showing reference, session details, and current status.

### Option 7 — View All Members

Lists all registered members with their IDs, names, and email addresses.

### Options 8 & 9 — Reports

See [Reports](#reports) section below.

---

## Sample Data

The `DataSeeder` pre-loads the system with:

| Category | Count | Details |
|---|---|---|
| Members | 10 | IDs `EFC-M01` to `EFC-M10` |
| Sessions | 48 | 8 weekends × 6 sessions each |
| Attended bookings | 22+ | Pre-seeded with ratings and comments |

**Pre-loaded members:**

| Member ID | Name | Email |
|---|---|---|
| EFC-M01 | Aarav Sharma | aarav.sharma@gym.co.uk |
| EFC-M02 | Priya Patel | priya.patel@gym.co.uk |
| EFC-M03 | Rohan Mehta | rohan.mehta@gym.co.uk |
| EFC-M04 | Ananya Iyer | ananya.iyer@gym.co.uk |
| EFC-M05 | Vikram Nair | vikram.nair@gym.co.uk |
| EFC-M06 | Deepika Reddy | deepika.reddy@gym.co.uk |
| EFC-M07 | Arjun Kapoor | arjun.kapoor@gym.co.uk |
| EFC-M08 | Sneha Krishnamurthy | sneha.krish@gym.co.uk |
| EFC-M09 | Karan Malhotra | karan.malhotra@gym.co.uk |
| EFC-M10 | Meera Joshi | meera.joshi@gym.co.uk |

**Session ID naming convention:**

```
W{week}{day}{slot}

Examples:
  W1SAM  →  Week 1, Saturday, Morning   (Pilates,  £14.00)
  W1SAD  →  Week 1, Saturday, Midday    (HIIT,     £12.00)
  W1SAE  →  Week 1, Saturday, Evening   (CrossFit, £13.50)
  W1SUM  →  Week 1, Sunday,   Morning   (Strength, £11.00)
  W1SUD  →  Week 1, Sunday,   Midday    (Yoga Flow,£10.00)
  W1SUE  →  Week 1, Sunday,   Evening   (Pilates,  £14.00)
```

**Class fees:**

| Fitness Type | Fee (£) |
|---|---|
| Pilates | £14.00 |
| CrossFit | £13.50 |
| HIIT | £12.00 |
| Strength Training | £11.00 |
| Yoga Flow | £10.00 |

---

## Reports

### Attendance Report (Option 8)

Displays all sessions that have at least one attendee, sorted by week, day, and time slot:

```
══════════════════════════════════════════════════════════════════════════════════════
        ELITE FITNESS CLUB — CLASS ATTENDANCE & RATINGS REPORT
══════════════════════════════════════════════════════════════════════════════════════
  Session    Fitness Type         Day        Slot       Week   Attendees  Avg Rating
  ──────────────────────────────────────────────────────────────────────────────────
  W1SAM      Pilates              Saturday   Morning    1      3          4.67 / 5
  W1SAD      HIIT                 Saturday   Midday     1      2          3.50 / 5
  ...
══════════════════════════════════════════════════════════════════════════════════════
```

### Revenue Report (Option 9)

Summarises total income per fitness type (attended bookings only), highlighting the top earner:

```
════════════════════════════════════════════════════════════
        ELITE FITNESS CLUB — REVENUE REPORT
════════════════════════════════════════════════════════════
  Pilates                   £    182.00  ◆ TOP EARNER
  CrossFit                  £    121.50
  HIIT                      £     96.00
  Strength Training         £     77.00
  Yoga Flow                 £     60.00
  ────────────────────────────────────────────────────────
  Champion class type : Pilates              (£182.00)
════════════════════════════════════════════════════════════
```

---

## Testing

The project includes a comprehensive JUnit 5 test suite (`EFCSystemTest.java`) covering:

- ✅ Booking placement — happy path and reference format validation
- 🚫 Capacity enforcement — max 4 members per session
- 🚫 Duplicate booking prevention
- 🚫 Time-slot conflict detection
- ✏️ Booking modification — valid swaps and edge cases
- ❌ Booking cancellation — status and spot-release verification
- 🎯 Attendance and feedback recording
- ⚠️ Feedback rating validation (rejects values outside 1–5)
- 📊 Attendance report generation
- 💰 Revenue report generation
- 🌱 DataSeeder verification — confirms 10 members, 48 sessions, 22+ reviews

**Run tests:**

```bash
mvn test
```

**Run tests with verbose output:**

```bash
mvn test -Dsurefire.useFile=false
```

---

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Core language (uses records, sealed classes, switch expressions) |
| Apache Maven | 3.6+ | Build and dependency management |
| JUnit Jupiter | 5.10.0 | Unit and integration testing |
| Maven Surefire Plugin | 3.1.2 | Test execution |
| Maven Assembly Plugin | 3.6.0 | Fat JAR packaging |

---

## License

This project is for educational purposes. All rights reserved © Elite Fitness Club.
