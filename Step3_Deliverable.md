# Step 3 — Database Implementation and Advanced Features

## 1. Sample Data Insertion

### 1.1 Prompt Used for Data Generation

> **Prompt given to the AI assistant:**
>
> *"Generate realistic sample data for the ArtConnect database. The data must cover all 10 entities and 4 junction tables. Include:*
> - *5 disciplines and 8 artwork tags*
> - *6 artists with varied profiles (different cities, active/inactive)*
> - *15 artworks spread across artists, with all 3 status values (FOR_SALE, SOLD, EXHIBITED)*
> - *4 galleries in different European cities*
> - *5 exhibitions with coherent date ranges*
> - *6 workshops at different levels (Beginner, Intermediate, Advanced)*
> - *8 community members (mix of free and premium)*
> - *12 bookings with varied payment statuses (PAID, PENDING, CANCELLED)*
> - *10 reviews with ratings from 1 to 5*
> - *Cross-participations: artworks appearing in multiple exhibitions, members attending multiple workshops*
> - *All many-to-many junction tables must be populated.*
>
> *The data should tell a coherent story and demonstrate interesting query results."*

### 1.2 Data Summary

| Entity | Count | Notes |
|--------|-------|-------|
| `discipline` | 5 | Painting, Sculpture, Photography, Digital Art, Mixed Media |
| `artwork_tag` | 8 | Abstract, Contemporary, Portrait, Landscape, Minimalist, Surrealist, Pop Art, Expressionist |
| `artist` | 6 | From Paris, Rome, Tokyo, Dakar, Stockholm, Brussels. 1 inactive. |
| `artwork` | 15 | 8 FOR_SALE, 4 SOLD, 3 EXHIBITED |
| `gallery` | 4 | Paris, Rome, Stockholm, London |
| `exhibition` | 5 | Dates spanning 2024-09 to 2025-09 |
| `workshop` | 6 | 2 Beginner, 2 Intermediate, 2 Advanced |
| `community_member` | 8 | 4 free, 4 premium |
| `booking` | 12 | 8 PAID, 2 PENDING, 2 CANCELLED |
| `review` | 10 | Ratings: 3×5★, 5×4★, 1×3★, 0×2★, 0×1★ |
| `artist_discipline` | 8 | Multi-disciplinary artists (Elena: 2, Amara: 2) |
| `member_favorite_discipline` | 12 | Members with 1-2 favorite disciplines each |
| `exhibition_artwork` | 13 | Cross-participations: artworks 3 and 7 appear in 2 expos |
| `artwork_artwork_tag` | 22 | Most artworks have 1-2 tags |

**Script:** [`artconnect_data.sql`](file:///c:/Users/Noah/Documents/VSCODE/SQL/PJ-DB-I1/artconnect_data.sql)

---

## 2. Views

### 2.1 `v_artist_portfolio` — Query Simplification

**Objective:** Avoid complex multi-join queries each time the artist portfolio page is loaded.

**What it shows:** Artist name, city, active status, list of disciplines, artwork count, and average artwork price.

```sql
SELECT * FROM v_artist_portfolio;
```

| artist_name | city | disciplines | artwork_count | avg_price |
|-------------|------|-------------|---------------|-----------|
| Elena Moreau | Paris | Mixed Media, Painting | 3 | 3500.00 |
| Marco Bellini | Rome | Sculpture | 3 | 15166.67 |
| Yuki Tanaka | Tokyo | Digital Art, Photography | 3 | 1733.33 |
| ... | ... | ... | ... | ... |

---

### 2.2 `v_exhibition_details` — Query Simplification

**Objective:** Centralize exhibition information with gallery details and artwork count for the exhibition listing page.

**What it shows:** Exhibition title, gallery name and city, date range, duration in days, curator, theme, artwork count.

```sql
SELECT * FROM v_exhibition_details;
```

| exhibition_title | gallery_name | duration_days | artwork_count |
|------------------|-------------|---------------|---------------|
| Visions of Tomorrow | Galerie Lumière | 105 | 3 |
| Global Threads | Gallery Horizons | 149 | 3 |
| ... | ... | ... | ... |

---

### 2.3 `v_workshop_availability` — Hiding Complexity

**Objective:** Encapsulate the available-spots calculation so the UI can query it directly.

**What it shows:** Workshop title, date, instructor, level, price, max participants, confirmed bookings (PAID/PENDING), available spots.

```sql
SELECT workshop_title, instructor_name, available_spots
FROM v_workshop_availability
WHERE available_spots > 0;
```

| workshop_title | instructor_name | available_spots |
|----------------|-----------------|-----------------|
| Introduction to Oil Painting | Elena Moreau | 9 |
| Night Photography Masterclass | Yuki Tanaka | 11 |
| ... | ... | ... |

---

### 2.4 `v_member_activity` — Security / Simplification

**Objective:** Provide member activity summary WITHOUT exposing sensitive data (email, phone).

**What it shows:** Member name, city, membership type, favorite disciplines, total bookings, total reviews.

```sql
SELECT * FROM v_member_activity;
```

| member_name | membership_type | favorite_disciplines | total_bookings | total_reviews |
|-------------|----------------|---------------------|----------------|---------------|
| Alice Martin | premium | Mixed Media, Painting | 1 | 2 |
| Ben Thompson | free | Photography, Sculpture | 2 | 2 |
| ... | ... | ... | ... | ... |

---

### 2.5 `v_artwork_catalog` — Query Simplification

**Objective:** Provide a ready-to-display artwork catalog with artist, tags, and average rating.

**What it shows:** Artwork title, year, type, medium, price, status, artist name, tags (comma-separated), average rating, review count.

```sql
SELECT artwork_title, artist_name, tags, avg_rating, price
FROM v_artwork_catalog
WHERE status = 'FOR_SALE'
ORDER BY avg_rating DESC;
```

**Script:** [`artconnect_views_indexes.sql`](file:///c:/Users/Noah/Documents/VSCODE/SQL/PJ-DB-I1/artconnect_views_indexes.sql)

---

## 3. Indexes

| # | Index Name | Table.Column(s) | Justification |
|---|-----------|-----------------|---------------|
| 1 | `idx_artwork_artist` | `artwork(artist_id)` | Accelerates JOINs between artwork and artist tables (used in portfolio view, catalog view, reports). FKs are not always auto-indexed by MySQL. |
| 2 | `idx_artwork_status` | `artwork(status)` | Frequent `WHERE status = 'FOR_SALE'` filtering in the application. Low cardinality (3 values) but very high access frequency. |
| 3 | `idx_exhibition_dates` | `exhibition(start_date, end_date)` | Composite index for range queries: "find exhibitions happening between X and Y" or "find currently active exhibitions." Both columns covered in a single B-tree scan. |
| 4 | `idx_review_artwork` | `review(artwork_id)` | Accelerates `AVG(rating)` calculation per artwork in `v_artwork_catalog` and detail pages. |
| 5 | `idx_booking_workshop` | `booking(workshop_id)` | Accelerates counting bookings per workshop (used in `v_workshop_availability` and the capacity-check trigger). |
| 6 | `idx_workshop_date` | `workshop(date)` | Workshops are frequently sorted and filtered by date (upcoming workshops, past workshops). Supports efficient `ORDER BY` and range scans. |

**Script:** [`artconnect_views_indexes.sql`](file:///c:/Users/Noah/Documents/VSCODE/SQL/PJ-DB-I1/artconnect_views_indexes.sql)

---

## 4. Triggers

### 4.1 `trg_check_workshop_capacity`

| Property | Value |
|----------|-------|
| **Event** | `BEFORE INSERT ON booking` |
| **Purpose** | Prevents overbooking by checking that the number of active bookings (PAID or PENDING) does not exceed `max_participants` |
| **Action on violation** | `SIGNAL SQLSTATE '45000'` with message *"Workshop is full: no more spots available."* |

**Test:**
```sql
-- Fill up workshop 2 (set max_participants = current active bookings)
UPDATE workshop SET max_participants = 1 WHERE workshop_id = 2;

-- This should fail:
INSERT INTO booking (workshop_id, member_id, payment_status)
VALUES (2, 3, 'PAID');
-- Error: Workshop is full: no more spots available.

-- Restore:
UPDATE workshop SET max_participants = 8 WHERE workshop_id = 2;
```

---

### 4.2 `trg_check_exhibition_date_consistency`

| Property | Value |
|----------|-------|
| **Event** | `BEFORE UPDATE ON exhibition` |
| **Purpose** | Prevents modifying exhibition dates so that `end_date < start_date` |
| **Action on violation** | `SIGNAL SQLSTATE '45000'` with message *"Exhibition end_date cannot be before start_date."* |

**Test:**
```sql
-- This should fail:
UPDATE exhibition SET end_date = '2024-08-01' WHERE exhibition_id = 1;
-- Error: Exhibition end_date cannot be before start_date.
```

---

### 4.3 `trg_audit_artwork_status`

| Property | Value |
|----------|-------|
| **Event** | `AFTER UPDATE ON artwork` |
| **Purpose** | Records every status change of an artwork into the `audit_log` table for traceability (e.g., tracking sales: FOR_SALE → SOLD) |
| **Action** | `INSERT INTO audit_log` with table name, record ID, old and new status, and timestamp |

**Test:**
```sql
UPDATE artwork SET status = 'SOLD' WHERE artwork_id = 1;

SELECT * FROM audit_log;
-- log_id=1, table_name='artwork', record_id=1,
-- action='STATUS_CHANGE', old_value='FOR_SALE', new_value='SOLD'
```

---

### 4.4 `trg_set_artwork_exhibited`

| Property | Value |
|----------|-------|
| **Event** | `AFTER INSERT ON exhibition_artwork` |
| **Purpose** | When an artwork is added to a currently active exhibition, automatically sets its status to `'EXHIBITED'` |
| **Action** | `UPDATE artwork SET status = 'EXHIBITED'` if `CURDATE()` is between the exhibition's `start_date` and `end_date` |

**Test:**
```sql
-- If an exhibition is currently active, adding an artwork will auto-update:
-- (depends on current date being within the exhibition's date range)
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES (4, 5);
SELECT status FROM artwork WHERE artwork_id = 5;
-- Should be 'EXHIBITED' if exhibition 4 is currently running
```

**Script:** [`artconnect_triggers.sql`](file:///c:/Users/Noah/Documents/VSCODE/SQL/PJ-DB-I1/artconnect_triggers.sql)

---

## 5. Stored Procedures & Functions

### 5.1 `fn_get_workshop_available_spots(workshop_id)` — Function

**Returns:** Number of remaining spots (`max_participants - active bookings`). Returns `NULL` if no limit.

```sql
SELECT fn_get_workshop_available_spots(1) AS spots;
-- Returns: 9 (12 max − 3 active bookings)

SELECT fn_get_workshop_available_spots(3) AS spots;
-- Returns: 11 (15 max − 4 active bookings)
```

---

### 5.2 `fn_get_artist_average_rating(artist_id)` — Function

**Returns:** Average review rating (DECIMAL 3,1) across all artworks of the given artist. `NULL` if no reviews.

```sql
SELECT fn_get_artist_average_rating(1) AS rating;
-- Returns: 4.3 (Elena Moreau: ratings 5, 4, 4 on her artworks)

SELECT fn_get_artist_average_rating(2) AS rating;
-- Returns: 4.5 (Marco Bellini: ratings 5, 4)
```

---

### 5.3 `sp_create_workshop_with_artist(...)` — Procedure

**Purpose:** Creates a workshop and, if the instructor (identified by email) doesn't exist, auto-creates the artist.

```sql
CALL sp_create_workshop_with_artist(
    'Intro to Watercolor',         -- title
    '2025-09-10 10:00:00',         -- date
    120,                           -- duration
    15,                            -- max participants
    55.00,                         -- price
    'Studio Central, Paris',       -- location
    'Discover watercolor basics.', -- description
    'Beginner',                    -- level
    'new.teacher@artmail.com',     -- instructor email
    'Jean Nouveau'                 -- instructor name (if new)
);
-- Returns: new_workshop_id, instructor_id
-- Also creates the artist "Jean Nouveau" if not already present.
```

---

### 5.4 `sp_register_member_to_workshops(member_id, workshop_ids)` — Procedure

**Purpose:** Batch-registers a member to multiple workshops (comma-separated IDs). Skips already-booked or full workshops.

```sql
CALL sp_register_member_to_workshops(1, '2,5,6');
-- Returns: workshops_registered = 3, workshops_skipped = 0
-- (assuming none are full and Alice isn't already booked)
```

---

### 5.5 `sp_generate_artist_report(artist_id)` — Procedure

**Purpose:** Comprehensive artist report — returns 4 result sets: info, artworks, exhibitions, workshops.

```sql
CALL sp_generate_artist_report(1);
-- Result set 1: Elena Moreau's info (name, city, disciplines, avg rating)
-- Result set 2: Her 3 artworks with tags and individual ratings
-- Result set 3: Exhibitions featuring her works
-- Result set 4: Workshops she instructs with available spots
```

**Script:** [`artconnect_procedures.sql`](file:///c:/Users/Noah/Documents/VSCODE/SQL/PJ-DB-I1/artconnect_procedures.sql)

---

## 6. Transactions

### 6.1 Scenario 1: Successful Atomic Registration (COMMIT)

**Story:** Léa Fontaine joins ArtConnect as a premium member. In one atomic transaction, she:
1. Creates her account (community_member)
2. Sets her favorite disciplines (Painting, Photography)
3. Books 3 workshops (Oil Painting, Night Photography, Geometric Abstraction)
4. Writes 2 reviews on artworks she visited

**Why atomicity matters:** If any step fails (e.g., a workshop is full or a review violates constraints), none of the operations should persist — the member shouldn't exist with partial data.

**Result:** All operations succeed → `COMMIT`. Léa exists with all her bookings, reviews, and preferences.

---

### 6.2 Scenario 2: Failed Atomic Registration (ROLLBACK)

**Story:** Marc Lefèvre tries to register and book 2 workshops, but one is full (the `trg_check_workshop_capacity` trigger fires). The entire transaction is rolled back.

**Implementation:** A stored procedure `sp_demo_atomic_registration` wraps the transaction with a `DECLARE EXIT HANDLER FOR SQLEXCEPTION` that executes `ROLLBACK` on any error.

**Result:** The trigger fires → the handler catches the error → `ROLLBACK`. Marc does not exist in the database.

**Script:** [`artconnect_transactions.sql`](file:///c:/Users/Noah/Documents/VSCODE/SQL/PJ-DB-I1/artconnect_transactions.sql)

---

## 7. Execution Order

To set up the complete ArtConnect database, run the scripts in this order:

```
1. artconnect_schema.sql          -- Tables and constraints
2. artconnect_data.sql            -- Sample data
3. artconnect_views_indexes.sql   -- Views and indexes
4. artconnect_triggers.sql        -- Audit table + triggers
5. artconnect_procedures.sql      -- Stored procedures & functions
6. artconnect_transactions.sql    -- Transaction tests
```
