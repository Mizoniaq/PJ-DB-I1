# Step 5 — Finalization & Presentation (ArtConnect Pro)

---

## Table of Contents

1. [Project Summary](#1-project-summary)
2. [Test Scenarios](#2-test-scenarios)
3. [Consolidated Deliverables (Steps 1–4)](#3-consolidated-deliverables-steps-14)
4. [Database Design — CDM, LDM, Normalization](#4-database-design--cdm-ldm-normalization)
5. [Advanced SQL Features](#5-advanced-sql-features)
6. [Application Architecture](#6-application-architecture)
7. [Challenges & Solutions](#7-challenges--solutions)
8. [How to Run the Application](#8-how-to-run-the-application)

---

## 1. Project Summary

**ArtConnect Pro** is a community platform that connects artists, showcases their works, organizes events (exhibitions, concerts, workshops), and lets community members discover artists and register for events.

The project was built in 5 steps over 6 supervised sessions:

| Step | Description | Status |
|------|-------------|--------|
| 1 | Functional analysis — UML use case + class diagrams | ✅ Complete |
| 2 | Conceptual & Logical modeling (CDM/LDM, 3NF, SQL schema) | ✅ Complete |
| 3 | DB implementation — views, indexes, triggers, procedures, transactions | ✅ Complete |
| 4 | JDBC integration — entities, DAOs, services, UI connected to MySQL | ✅ Complete |
| 5 | Finalization — tests, refinements, documentation, presentation | ✅ Complete |

**Tech stack:** Java 23 · JavaFX 23 · MySQL 8 · JDBC (HikariCP connection pool) · Maven

---

## 2. Test Scenarios

The following scenarios were tested to verify database–application consistency.

### 2.1 Artist CRUD

| Scenario | Expected Result | Status |
|----------|----------------|--------|
| Load all artists → Artists tab | List populated from MySQL `artist` + `artist_discipline` tables | ✅ |
| Search artist by name | Filters the list in real time from DB data | ✅ |
| Filter by discipline | Calls `searchArtists(query, discipline, city)` → SQL WHERE on joined discipline table | ✅ |
| Create artist with 2 disciplines | INSERT into `artist` + 2 rows in `artist_discipline` — transactional | ✅ |
| Update artist (city change) | UPDATE row in `artist`, disciplines refreshed (DELETE + re-INSERT) | ✅ |
| Delete artist | DELETE cascades to `artwork`, `artist_discipline`, `workshop` (RESTRICT) | ✅ |

### 2.2 Artwork CRUD

| Scenario | Expected Result | Status |
|----------|----------------|--------|
| Load all artworks | JOINs `artwork` with `artist` to reconstruct artist object | ✅ |
| Filter by status FOR_SALE | Uses `idx_artwork_status` index | ✅ |
| Create artwork linked to artist | INSERT with subquery `(SELECT artist_id FROM artist WHERE name = ?)` | ✅ |
| Update artwork status to SOLD | `trg_audit_artwork_status` fires → row inserted in `audit_log` | ✅ |
| Add exhibited artwork to active exhibition | `trg_set_artwork_exhibited` fires → status auto-set to EXHIBITED | ✅ |

### 2.3 Gallery & Exhibition

| Scenario | Expected Result | Status |
|----------|----------------|--------|
| Load all galleries | Gallery list with exhibitions loaded via cascade | ✅ |
| Load exhibitions tab | JOINs `exhibition` with `gallery` + loads artworks via junction table | ✅ |
| Try to set end_date before start_date | `trg_check_exhibition_date_consistency` blocks the UPDATE | ✅ |

### 2.4 Workshop & Booking

| Scenario | Expected Result | Status |
|----------|----------------|--------|
| Load all workshops | JOIN with `artist` to reconstruct instructor | ✅ |
| Book a workshop | INSERT into `booking`, triggers check for capacity | ✅ |
| Try to overbook (exceed max_participants) | `trg_check_workshop_capacity` fires → INSERT blocked with error message | ✅ |
| View bookings for a member | Loads from `booking` JOIN `workshop` | ✅ |

### 2.5 Community Members

| Scenario | Expected Result | Status |
|----------|----------------|--------|
| Load all members | Loads `community_member` + bookings + reviews per member | ✅ |
| Create member with favorite disciplines | INSERT into `community_member` + `member_favorite_discipline` — transactional | ✅ |
| Update member (city, membership upgrade) | UPDATE + refresh disciplines (transactional) | ✅ |
| Delete member | CASCADE removes bookings, reviews, favorite disciplines | ✅ |

### 2.6 Transactions

| Scenario | Expected Result | Status |
|----------|----------------|--------|
| Atomic member registration (multi-table) | All inserts succeed → COMMIT | ✅ |
| Atomic registration with full workshop | Trigger fires → handler catches → ROLLBACK → member does not exist | ✅ |

### 2.7 Stored Procedures

| Procedure/Function | Test | Result |
|-------------------|------|--------|
| `fn_get_workshop_available_spots(1)` | Returns max_participants − active bookings | ✅ |
| `fn_get_artist_average_rating(1)` | Returns average rating across all artworks | ✅ |
| `sp_create_workshop_with_artist(...)` | Creates workshop + auto-creates artist if email not found | ✅ |
| `sp_register_member_to_workshops(1, '2,5,6')` | Batch-registers, skips full or already-booked | ✅ |
| `sp_generate_artist_report(1)` | Returns 4 result sets: info, artworks, exhibitions, workshops | ✅ |

---

## 3. Consolidated Deliverables (Steps 1–4)

| Step | Deliverable | File |
|------|-------------|------|
| 1 | Feature list + UML use case diagram + class diagrams | `Step1_Deliverable.md` |
| 1 | Use case diagram (PNG) | `Use Case Diagram.png` |
| 1 | Domain model diagram (PNG) | `Model Layer - Domain Classes.png` |
| 1 | Architecture layer diagram (PNG) | `Architecture - Layer Diagram.png` |
| 2 | CDM (ERD) diagram | `Conceptual Data Model - ERD.png` |
| 2 | LDM (tables, keys, types) + normalization explanation | `Step2_Deliverable.md` |
| 2 | LDM diagram (PNG) | `LDM Diagram.png` |
| 2 | SQL schema creation script | `artconnect_schema.sql` |
| 3 | Sample data insertion script + LLM prompt | `Step3_Deliverable.md` §1, `artconnect_data.sql` |
| 3 | Views & Indexes script | `artconnect_views_indexes.sql` |
| 3 | Triggers script | `artconnect_triggers.sql` |
| 3 | Stored procedures & functions script | `artconnect_procedures.sql` |
| 3 | Transaction test script | `artconnect_transactions.sql` |
| 3 | Documentation (views, triggers, procedures) | `Step3_Deliverable.md` §2–6 |
| 4 | Entities, DAOs, services (Java code) | `ArtConnectPro-App/src/` |
| 4 | Architecture description + layer schema | `Step4_Deliverable.md` |
| 4 | Application screenshots (7) | `screenshot_*.png` |

---

## 4. Database Design — CDM, LDM, Normalization

### 4.1 Entities (10 total)

`Artist` · `Artwork` · `Gallery` · `Exhibition` · `Workshop` · `CommunityMember` · `Booking` · `Review` · `Discipline` · `ArtworkTag`

### 4.2 Relationships

| Relationship | Type | Junction Table |
|---|---|---|
| Artist ↔ Artwork | 1:N | — |
| Artist ↔ Discipline | N:M | `artist_discipline` |
| Artist ↔ Workshop (instructor) | 1:N | — |
| Artwork ↔ ArtworkTag | N:M | `artwork_artwork_tag` |
| Artwork ↔ Review | 1:N | — |
| Exhibition ↔ Gallery | N:1 | — |
| Exhibition ↔ Artwork | N:M | `exhibition_artwork` |
| Workshop ↔ CommunityMember | N:M | `booking` |
| CommunityMember ↔ Discipline | N:M | `member_favorite_discipline` |
| CommunityMember ↔ Review | 1:N | — |

**Total: 14 tables** (10 core + 4 junction)

### 4.3 Normalization (3NF)

**1NF — Atomic values:**
All attributes are atomic (no comma-separated lists in columns). Disciplines and tags that were stored as lists in Java become separate tables `discipline` and `artwork_tag`, linked via junction tables.

**2NF — No partial dependencies:**
All tables use single-column surrogate keys (`INT AUTO_INCREMENT`). No composite-key tables with partial dependencies. Junction tables (`artist_discipline`, `exhibition_artwork`, etc.) contain only their two foreign keys (+ metadata like `booking_date`).

**3NF — No transitive dependencies:**
`artwork.artist_id` is a FK, not a repeated artist name. `exhibition.gallery_id` is a FK. No non-key attribute depends on another non-key attribute. Example: `contact_email` and `city` both depend only on `artist_id`, not on each other.

**Key constraints:**
- `artist.contact_email` — UNIQUE
- `community_member.email` — UNIQUE
- `discipline.name` — UNIQUE
- `artwork_tag.name` — UNIQUE
- `booking(workshop_id, member_id)` — UNIQUE (no double booking)
- `exhibition.end_date >= start_date` — CHECK
- `review.rating BETWEEN 1 AND 5` — CHECK
- `membership_type IN ('free','premium')` — CHECK

---

## 5. Advanced SQL Features

### 6.1 Views (5)

| View | Objective |
|------|-----------|
| `v_artist_portfolio` | Query simplification — artist info + disciplines (GROUP_CONCAT) + artwork count + avg price |
| `v_exhibition_details` | Query simplification — exhibition + gallery + artwork count + duration in days |
| `v_workshop_availability` | Complexity hiding — available spots calculation (max − active bookings) |
| `v_member_activity` | Security — member summary WITHOUT email/phone; exposes only public activity data |
| `v_artwork_catalog` | Query simplification — artwork + artist + tags + avg rating for frontend display |

### 6.2 Indexes (6)

| Index | Table.Column | Reason |
|-------|-------------|--------|
| `idx_artwork_artist` | `artwork(artist_id)` | FK join between artwork and artist (portfolio, catalog) |
| `idx_artwork_status` | `artwork(status)` | Frequent WHERE status = 'FOR_SALE' filtering |
| `idx_exhibition_dates` | `exhibition(start_date, end_date)` | Composite index for date range queries |
| `idx_review_artwork` | `review(artwork_id)` | AVG(rating) aggregations in catalog view |
| `idx_booking_workshop` | `booking(workshop_id)` | Booking count per workshop (capacity trigger) |
| `idx_workshop_date` | `workshop(date)` | Sort and filter workshops by date |

### 6.3 Triggers (4)

| Trigger | Event | Purpose |
|---------|-------|---------|
| `trg_check_workshop_capacity` | BEFORE INSERT ON booking | Blocks overbooking (PAID + PENDING ≥ max_participants) |
| `trg_check_exhibition_date_consistency` | BEFORE UPDATE ON exhibition | Blocks end_date < start_date |
| `trg_audit_artwork_status` | AFTER UPDATE ON artwork | Logs every status change to `audit_log` table |
| `trg_set_artwork_exhibited` | AFTER INSERT ON exhibition_artwork | Auto-sets artwork status to EXHIBITED if exhibition is currently active |

### 6.4 Stored Procedures & Functions (5)

| Object | Type | Purpose |
|--------|------|---------|
| `fn_get_workshop_available_spots(id)` | FUNCTION | Returns remaining spots for a workshop |
| `fn_get_artist_average_rating(id)` | FUNCTION | Returns average rating across all artist's artworks |
| `sp_create_workshop_with_artist(...)` | PROCEDURE | Creates workshop + auto-creates artist if not found by email |
| `sp_register_member_to_workshops(id, ids)` | PROCEDURE | Batch-registers a member to multiple workshops, skips full/already-booked |
| `sp_generate_artist_report(id)` | PROCEDURE | Returns 4 result sets: artist info, artworks, exhibitions, workshops |

### 6.5 Transactions (2 scenarios)

**Scenario 1 — COMMIT:** Atomic registration of a new premium member: creates member → sets favorite disciplines → books 3 workshops → writes 2 reviews. All-or-nothing.

**Scenario 2 — ROLLBACK:** Attempted registration where one workshop is full. The capacity trigger fires a SQLSTATE error, caught by `DECLARE EXIT HANDLER FOR SQLEXCEPTION` → full ROLLBACK. The member is never created.

---

## 6. Application Architecture

### 7.1 Layered Architecture (3-Tier)

```
┌──────────────────────────────────────────────────────┐
│                  UI Layer (JavaFX)                    │
│     8 Controllers + 8 FXML views                     │
│  ArtistController, ArtworkController, ...            │
└─────────────────────┬────────────────────────────────┘
                      │ uses (via ServiceProvider)
┌─────────────────────▼────────────────────────────────┐
│              Service Layer (interfaces)               │
│  ArtistService, ArtworkService, GalleryService,      │
│  WorkshopService, CommunityService                   │
│                                                      │
│  Implementations: JdbcArtistService,                 │
│  JdbcArtworkService, JdbcGalleryService,             │
│  JdbcWorkshopService, JdbcCommunityService           │
└─────────────────────┬────────────────────────────────┘
                      │ delegates to
┌─────────────────────▼────────────────────────────────┐
│           DAO / Persistence Layer (JDBC)              │
│  Interfaces: ArtistDao, ArtworkDao, ExhibitionDao,   │
│  GalleryDao, WorkshopDao, CommunityMemberDao         │
│                                                      │
│  Implementations: JdbcArtistDao, JdbcArtworkDao,     │
│  JdbcExhibitionDao, JdbcGalleryDao,                  │
│  JdbcWorkshopDao, JdbcCommunityMemberDao             │
└─────────────────────┬────────────────────────────────┘
                      │ HikariCP pool (max 10 connections)
┌─────────────────────▼────────────────────────────────┐
│          Database Layer (MySQL — artconnect)          │
│  14 tables · 5 views · 6 indexes                     │
│  4 triggers · 5 stored programs                      │
└──────────────────────────────────────────────────────┘
```

### 7.2 Key Design Decisions

**OOP-first model:** Java entity classes have no `id` fields — they use direct object references (Artist has `List<Artwork>`, Exhibition has `Gallery`). Database IDs exist only at the SQL level (surrogate `INT AUTO_INCREMENT`). DAOs resolve IDs via name/email lookups when writing.

**Manual dependency injection:** `ServiceProvider.java` acts as a singleton factory that instantiates all JDBC DAOs and wires them into services. No Spring/CDI framework needed.

**InMemory services kept:** `InMemoryArtistService`, `InMemoryArtworkService`, etc. are preserved for offline testing without a database.

**PreparedStatement everywhere:** All SQL in JDBC DAOs uses `PreparedStatement` to prevent SQL injection. All connections use `try-with-resources` for guaranteed resource cleanup.

**Transactions on multi-table writes:**
- `JdbcArtistDao.save/update` — atomic insert into `artist` + `artist_discipline`
- `JdbcExhibitionDao.save` — atomic insert into `exhibition` + `exhibition_artwork`
- `JdbcCommunityMemberDao.save/update` — atomic insert into `community_member` + `member_favorite_discipline`

### 7.3 Package Structure

```
com.project.artconnect
├── MainApp.java
├── config/
│   └── DatabaseConfig.java          ← loads database.properties
├── model/                           ← 10 POJO entity classes
├── dao/                             ← 6 DAO interfaces (full CRUD)
├── persistence/                     ← 6 JdbcXxxDao implementations
├── service/                         ← 5 service interfaces
│   └── impl/                        ← 5 Jdbc + 5 InMemory implementations
├── ui/                              ← 8 JavaFX controllers
└── util/
    ├── ConnectionManager.java       ← HikariCP pool
    └── ServiceProvider.java         ← manual DI factory
```

---

## 7. Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| **No IDs in Java model classes** | Surrogate `INT AUTO_INCREMENT` PKs in DB only. DAOs resolve IDs via unique fields (name, email) before writing. |
| **N:M relationships in Java are simple Lists** | Junction tables (`artist_discipline`, `exhibition_artwork`, etc.) in DB. DAOs do batch inserts/deletes in a single transaction. |
| **Object reconstruction from flat SQL rows** | Each DAO has a `mapRow(ResultSet)` helper. For multi-table data, batch queries load children separately and match them by ID (e.g., `loadDisciplinesForArtists(Map<id, Artist>)`). |
| **One new connection per query (raw DriverManager)** | Replaced with **HikariCP connection pool** (pool size 2–10). Connections are reused across queries. |
| **Date null safety in Exhibition** | `Date.valueOf()` throws NPE on null. Fixed with ternary guard: `date != null ? Date.valueOf(date) : null`. |
| **Workshop instructor null safety** | Added an `IllegalArgumentException` guard before opening the connection in `save()` and `update()`. |
| **InMemory vs JDBC services** | `ServiceProvider` toggles between implementations. Currently hardwired to JDBC. InMemory kept for offline dev/testing. |

---

## 8. How to Run the Application

### Prerequisites
- Java 23
- MySQL 8 running locally
- Maven (or use IDE build)

### Setup

```bash
# 1. Create and populate the database (run scripts in order):
mysql -u root -p < artconnect_schema.sql
mysql -u root -p < artconnect_data.sql
mysql -u root -p < artconnect_views_indexes.sql
mysql -u root -p < artconnect_triggers.sql
mysql -u root -p < artconnect_procedures.sql
mysql -u root -p < artconnect_transactions.sql

# 2. Configure database credentials:
# Edit ArtConnectPro-App/src/main/resources/database.properties
db.url=jdbc:mysql://localhost:3306/artconnect
db.user=root
db.password=YOUR_PASSWORD

# 3. Build and run:
cd ArtConnectPro-App
mvn clean compile
mvn javafx:run
```

### Application Screenshots

| Tab | Screenshot |
|-----|-----------|
| Discover | ![Discover](screenshot_discover.png) |
| Artists | ![Artists](screenshot_artists.png) |
| Artworks | ![Artworks](screenshot_artworks.png) |
| Galleries | ![Galleries](screenshot_galleries.png) |
| Exhibitions | ![Exhibitions](screenshot_exhibitions.png) |
| Workshops | ![Workshops](screenshot_workshops.png) |
| Community | ![Community](screenshot_community.png) |

---