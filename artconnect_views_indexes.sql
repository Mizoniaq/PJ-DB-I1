-- =============================================================
-- ArtConnect — Views & Indexes
-- Step 3, Task 2: Views for query simplification and security,
--                  Indexes for query optimization
-- =============================================================
-- Prerequisites: Run artconnect_schema.sql + artconnect_data.sql
-- =============================================================

USE artconnect;

-- =============================================================
-- VIEWS
-- =============================================================

-- -------------------------------------------------------------
-- VIEW 1: v_artist_portfolio
-- Objective: QUERY SIMPLIFICATION
-- Shows each artist with their discipline list, artwork count,
-- and average artwork price. Avoids a complex multi-join query
-- every time the portfolio page is loaded.
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_artist_portfolio AS
SELECT
    a.artist_id,
    a.name                  AS artist_name,
    a.city,
    a.is_active,
    GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ')
                            AS disciplines,
    COUNT(DISTINCT aw.artwork_id)
                            AS artwork_count,
    COALESCE(ROUND(AVG(aw.price), 2), 0)
                            AS avg_price
FROM artist a
LEFT JOIN artist_discipline ad ON a.artist_id = ad.artist_id
LEFT JOIN discipline d         ON ad.discipline_id = d.discipline_id
LEFT JOIN artwork aw           ON a.artist_id = aw.artist_id
GROUP BY a.artist_id, a.name, a.city, a.is_active;

-- -------------------------------------------------------------
-- VIEW 2: v_exhibition_details
-- Objective: QUERY SIMPLIFICATION
-- Centralizes exhibition info with gallery name, artwork count,
-- and exhibition duration. Used by the exhibition listing page.
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_exhibition_details AS
SELECT
    e.exhibition_id,
    e.title                 AS exhibition_title,
    g.name                  AS gallery_name,
    g.city                  AS gallery_city,
    e.start_date,
    e.end_date,
    DATEDIFF(e.end_date, e.start_date) AS duration_days,
    e.curator_name,
    e.theme,
    COUNT(ea.artwork_id)    AS artwork_count
FROM exhibition e
JOIN gallery g             ON e.gallery_id = g.gallery_id
LEFT JOIN exhibition_artwork ea ON e.exhibition_id = ea.exhibition_id
GROUP BY e.exhibition_id, e.title, g.name, g.city,
         e.start_date, e.end_date, e.curator_name, e.theme;

-- -------------------------------------------------------------
-- VIEW 3: v_workshop_availability
-- Objective: HIDING COMPLEXITY
-- Encapsulates the available-spots calculation so that the UI
-- can simply query this view instead of computing it each time.
-- Only counts bookings that are PAID or PENDING (not CANCELLED).
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_workshop_availability AS
SELECT
    w.workshop_id,
    w.title                 AS workshop_title,
    w.date                  AS workshop_date,
    w.level,
    w.price,
    a.name                  AS instructor_name,
    w.location,
    w.max_participants,
    COUNT(CASE WHEN b.payment_status IN ('PAID', 'PENDING')
               THEN 1 END) AS confirmed_bookings,
    w.max_participants
      - COUNT(CASE WHEN b.payment_status IN ('PAID', 'PENDING')
                   THEN 1 END)
                            AS available_spots
FROM workshop w
JOIN artist a              ON w.instructor_id = a.artist_id
LEFT JOIN booking b        ON w.workshop_id = b.workshop_id
GROUP BY w.workshop_id, w.title, w.date, w.level, w.price,
         a.name, w.location, w.max_participants;

-- -------------------------------------------------------------
-- VIEW 4: v_member_activity
-- Objective: SECURITY / SIMPLIFICATION
-- Provides a summary of each member's activity (bookings,
-- reviews, favorite disciplines) WITHOUT exposing sensitive
-- columns like email and phone number.
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_member_activity AS
SELECT
    cm.member_id,
    cm.name                 AS member_name,
    cm.city,
    cm.membership_type,
    GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ')
                            AS favorite_disciplines,
    COUNT(DISTINCT b.booking_id)
                            AS total_bookings,
    COUNT(DISTINCT r.review_id)
                            AS total_reviews
FROM community_member cm
LEFT JOIN member_favorite_discipline mfd ON cm.member_id = mfd.member_id
LEFT JOIN discipline d                   ON mfd.discipline_id = d.discipline_id
LEFT JOIN booking b                      ON cm.member_id = b.member_id
LEFT JOIN review r                       ON cm.member_id = r.reviewer_id
GROUP BY cm.member_id, cm.name, cm.city, cm.membership_type;

-- -------------------------------------------------------------
-- VIEW 5: v_artwork_catalog
-- Objective: QUERY SIMPLIFICATION
-- Full artwork catalog with artist name, tags, and average
-- review rating. Ready for frontend display / search.
-- -------------------------------------------------------------
CREATE OR REPLACE VIEW v_artwork_catalog AS
SELECT
    aw.artwork_id,
    aw.title                AS artwork_title,
    aw.creation_year,
    aw.type,
    aw.medium,
    aw.dimensions,
    aw.price,
    aw.status,
    a.name                  AS artist_name,
    GROUP_CONCAT(DISTINCT t.name ORDER BY t.name SEPARATOR ', ')
                            AS tags,
    COALESCE(ROUND(AVG(r.rating), 1), NULL)
                            AS avg_rating,
    COUNT(DISTINCT r.review_id)
                            AS review_count
FROM artwork aw
JOIN artist a              ON aw.artist_id = a.artist_id
LEFT JOIN artwork_artwork_tag aat ON aw.artwork_id = aat.artwork_id
LEFT JOIN artwork_tag t           ON aat.tag_id = t.tag_id
LEFT JOIN review r                ON aw.artwork_id = r.artwork_id
GROUP BY aw.artwork_id, aw.title, aw.creation_year, aw.type,
         aw.medium, aw.dimensions, aw.price, aw.status, a.name;


-- =============================================================
-- INDEXES
-- =============================================================
-- Note: Primary keys and UNIQUE constraints already create
-- implicit indexes. The indexes below target non-key columns
-- used frequently in WHERE, JOIN, or ORDER BY clauses.
-- =============================================================

-- -------------------------------------------------------------
-- INDEX 1: idx_artwork_artist
-- Table: artwork(artist_id)
-- Justification: The FK artwork.artist_id is used in virtually
-- every query that joins artworks with their artist (portfolio
-- view, catalog view, etc.). MySQL may or may not auto-index
-- FKs depending on the storage engine; this ensures it exists.
-- -------------------------------------------------------------
CREATE INDEX idx_artwork_artist ON artwork(artist_id);

-- -------------------------------------------------------------
-- INDEX 2: idx_artwork_status
-- Table: artwork(status)
-- Justification: Frequent filtering by status (e.g., "show all
-- artworks FOR_SALE"). Low cardinality (3 values) but very
-- frequent access pattern in the application.
-- -------------------------------------------------------------
CREATE INDEX idx_artwork_status ON artwork(status);

-- -------------------------------------------------------------
-- INDEX 3: idx_exhibition_dates
-- Table: exhibition(start_date, end_date)
-- Justification: Composite index for range queries like
-- "find exhibitions happening between date X and date Y" or
-- "find current exhibitions" (WHERE start_date <= NOW()
-- AND end_date >= NOW()). Covering both columns in one index
-- avoids two separate scans.
-- -------------------------------------------------------------
CREATE INDEX idx_exhibition_dates ON exhibition(start_date, end_date);

-- -------------------------------------------------------------
-- INDEX 4: idx_review_artwork
-- Table: review(artwork_id)
-- Justification: Accelerates the AVG(rating) calculation per
-- artwork, which is used in the v_artwork_catalog view and
-- whenever an artwork's detail page is loaded.
-- -------------------------------------------------------------
CREATE INDEX idx_review_artwork ON review(artwork_id);

-- -------------------------------------------------------------
-- INDEX 5: idx_booking_workshop
-- Table: booking(workshop_id)
-- Justification: Accelerates counting bookings per workshop
-- (used in v_workshop_availability). Also speeds up the
-- capacity-check trigger.
-- -------------------------------------------------------------
CREATE INDEX idx_booking_workshop ON booking(workshop_id);

-- -------------------------------------------------------------
-- INDEX 6: idx_workshop_date
-- Table: workshop(date)
-- Justification: Workshops are frequently sorted and filtered
-- by date (upcoming workshops, past workshops). This index
-- supports efficient ORDER BY and range scans.
-- -------------------------------------------------------------
CREATE INDEX idx_workshop_date ON workshop(date);
