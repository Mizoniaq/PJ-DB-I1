-- =============================================================
-- ArtConnect — Stored Procedures & Functions
-- Step 3, Task 3b: Common operations encapsulated as reusable
--                   database programs
-- =============================================================
-- Prerequisites: Run artconnect_schema.sql + artconnect_data.sql
-- =============================================================

USE artconnect;

-- Drop procedures/functions if they exist (safe re-run)
DROP FUNCTION  IF EXISTS fn_get_workshop_available_spots;
DROP FUNCTION  IF EXISTS fn_get_artist_average_rating;
DROP PROCEDURE IF EXISTS sp_create_workshop_with_artist;
DROP PROCEDURE IF EXISTS sp_register_member_to_workshops;
DROP PROCEDURE IF EXISTS sp_generate_artist_report;

-- =============================================================
-- FUNCTION 1: fn_get_workshop_available_spots
-- Returns the number of remaining spots for a given workshop.
-- Usage: SELECT fn_get_workshop_available_spots(1);
-- =============================================================

DELIMITER //

CREATE FUNCTION fn_get_workshop_available_spots(p_workshop_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_max      INT;
    DECLARE v_booked   INT;

    SELECT max_participants INTO v_max
    FROM workshop
    WHERE workshop_id = p_workshop_id;

    -- If workshop not found or has no limit
    IF v_max IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT COUNT(*) INTO v_booked
    FROM booking
    WHERE workshop_id = p_workshop_id
      AND payment_status IN ('PAID', 'PENDING');

    RETURN v_max - v_booked;
END //

DELIMITER ;

-- =============================================================
-- FUNCTION 2: fn_get_artist_average_rating
-- Returns the average review rating across all artworks
-- of a given artist. Returns NULL if no reviews exist.
-- Usage: SELECT fn_get_artist_average_rating(1);
-- =============================================================

DELIMITER //

CREATE FUNCTION fn_get_artist_average_rating(p_artist_id INT)
RETURNS DECIMAL(3,1)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_avg DECIMAL(3,1);

    SELECT ROUND(AVG(r.rating), 1) INTO v_avg
    FROM review r
    JOIN artwork aw ON r.artwork_id = aw.artwork_id
    WHERE aw.artist_id = p_artist_id;

    RETURN v_avg;
END //

DELIMITER ;

-- =============================================================
-- PROCEDURE 1: sp_create_workshop_with_artist
-- Creates a new workshop. If the specified instructor (by email)
-- does not already exist, creates the artist record first and
-- then uses the new artist_id as instructor_id.
-- Usage:
--   CALL sp_create_workshop_with_artist(
--       'Watercolor Basics',           -- title
--       '2025-09-10 10:00:00',         -- date
--       120,                           -- duration_minutes
--       15,                            -- max_participants
--       55.00,                         -- price
--       'Studio Central, Paris',       -- location
--       'Intro to watercolor',         -- description
--       'Beginner',                    -- level
--       'new.artist@artmail.com',      -- instructor email
--       'New Artist'                   -- instructor name (used if creating)
--   );
-- =============================================================

DELIMITER //

CREATE PROCEDURE sp_create_workshop_with_artist(
    IN p_title          VARCHAR(255),
    IN p_date           DATETIME,
    IN p_duration       INT,
    IN p_max_parts      INT,
    IN p_price          DECIMAL(10,2),
    IN p_location       VARCHAR(255),
    IN p_description    TEXT,
    IN p_level          VARCHAR(50),
    IN p_instructor_email VARCHAR(255),
    IN p_instructor_name  VARCHAR(255)
)
BEGIN
    DECLARE v_artist_id INT;

    -- Check if artist already exists
    SELECT artist_id INTO v_artist_id
    FROM artist
    WHERE contact_email = p_instructor_email
    LIMIT 1;

    -- If not, create the artist
    IF v_artist_id IS NULL THEN
        INSERT INTO artist (name, contact_email, is_active)
        VALUES (p_instructor_name, p_instructor_email, TRUE);

        SET v_artist_id = LAST_INSERT_ID();
    END IF;

    -- Create the workshop
    INSERT INTO workshop (title, date, duration_minutes, max_participants,
                          price, instructor_id, location, description, level)
    VALUES (p_title, p_date, p_duration, p_max_parts,
            p_price, v_artist_id, p_location, p_description, p_level);

    SELECT LAST_INSERT_ID() AS new_workshop_id, v_artist_id AS instructor_id;
END //

DELIMITER ;

-- =============================================================
-- PROCEDURE 2: sp_register_member_to_workshops
-- Registers a member to one or more workshops at once.
-- Accepts a comma-separated list of workshop IDs.
-- Uses a loop to insert bookings one by one, skipping
-- workshops that are already booked or full.
-- Usage:
--   CALL sp_register_member_to_workshops(1, '1,3,5');
-- =============================================================

DELIMITER //

CREATE PROCEDURE sp_register_member_to_workshops(
    IN p_member_id    INT,
    IN p_workshop_ids TEXT
)
BEGIN
    DECLARE v_ws_id       INT;
    DECLARE v_pos         INT DEFAULT 1;
    DECLARE v_next_comma  INT;
    DECLARE v_token       VARCHAR(20);
    DECLARE v_already     INT;
    DECLARE v_spots       INT;
    DECLARE v_registered  INT DEFAULT 0;
    DECLARE v_skipped     INT DEFAULT 0;

    -- Parse the comma-separated list
    parse_loop: LOOP
        SET v_next_comma = LOCATE(',', p_workshop_ids, v_pos);

        IF v_next_comma = 0 THEN
            SET v_token = TRIM(SUBSTRING(p_workshop_ids, v_pos));
        ELSE
            SET v_token = TRIM(SUBSTRING(p_workshop_ids, v_pos, v_next_comma - v_pos));
        END IF;

        -- Exit if token is empty
        IF v_token = '' OR v_token IS NULL THEN
            LEAVE parse_loop;
        END IF;

        SET v_ws_id = CAST(v_token AS UNSIGNED);

        -- Check if already booked
        SELECT COUNT(*) INTO v_already
        FROM booking
        WHERE workshop_id = v_ws_id AND member_id = p_member_id;

        IF v_already = 0 THEN
            -- Check availability using our function
            SET v_spots = fn_get_workshop_available_spots(v_ws_id);

            IF v_spots IS NULL OR v_spots > 0 THEN
                INSERT INTO booking (workshop_id, member_id, payment_status)
                VALUES (v_ws_id, p_member_id, 'PENDING');
                SET v_registered = v_registered + 1;
            ELSE
                SET v_skipped = v_skipped + 1;
            END IF;
        ELSE
            SET v_skipped = v_skipped + 1;
        END IF;

        -- Move to next token
        IF v_next_comma = 0 THEN
            LEAVE parse_loop;
        END IF;
        SET v_pos = v_next_comma + 1;
    END LOOP parse_loop;

    SELECT v_registered AS workshops_registered,
           v_skipped    AS workshops_skipped;
END //

DELIMITER ;

-- =============================================================
-- PROCEDURE 3: sp_generate_artist_report
-- Displays a comprehensive report for a given artist:
-- - Basic info & disciplines
-- - All artworks with status
-- - Exhibitions featuring their works
-- - Workshops they instruct
-- - Average rating across all their artworks
-- Usage: CALL sp_generate_artist_report(1);
-- =============================================================

DELIMITER //

CREATE PROCEDURE sp_generate_artist_report(
    IN p_artist_id INT
)
BEGIN
    -- 1. Artist info
    SELECT
        a.artist_id,
        a.name,
        a.city,
        a.contact_email,
        a.is_active,
        GROUP_CONCAT(DISTINCT d.name ORDER BY d.name SEPARATOR ', ') AS disciplines,
        fn_get_artist_average_rating(p_artist_id) AS overall_avg_rating
    FROM artist a
    LEFT JOIN artist_discipline ad ON a.artist_id = ad.artist_id
    LEFT JOIN discipline d         ON ad.discipline_id = d.discipline_id
    WHERE a.artist_id = p_artist_id
    GROUP BY a.artist_id, a.name, a.city, a.contact_email, a.is_active;

    -- 2. Artworks
    SELECT
        aw.artwork_id,
        aw.title,
        aw.creation_year,
        aw.type,
        aw.medium,
        aw.price,
        aw.status,
        GROUP_CONCAT(DISTINCT t.name ORDER BY t.name SEPARATOR ', ') AS tags,
        COALESCE(ROUND(AVG(r.rating), 1), 0) AS avg_rating,
        COUNT(DISTINCT r.review_id) AS review_count
    FROM artwork aw
    LEFT JOIN artwork_artwork_tag aat ON aw.artwork_id = aat.artwork_id
    LEFT JOIN artwork_tag t           ON aat.tag_id = t.tag_id
    LEFT JOIN review r                ON aw.artwork_id = r.artwork_id
    WHERE aw.artist_id = p_artist_id
    GROUP BY aw.artwork_id, aw.title, aw.creation_year,
             aw.type, aw.medium, aw.price, aw.status;

    -- 3. Exhibitions featuring this artist's works
    SELECT DISTINCT
        e.exhibition_id,
        e.title AS exhibition_title,
        g.name  AS gallery_name,
        e.start_date,
        e.end_date,
        e.theme
    FROM exhibition e
    JOIN gallery g            ON e.gallery_id = g.gallery_id
    JOIN exhibition_artwork ea ON e.exhibition_id = ea.exhibition_id
    JOIN artwork aw            ON ea.artwork_id = aw.artwork_id
    WHERE aw.artist_id = p_artist_id;

    -- 4. Workshops instructed
    SELECT
        w.workshop_id,
        w.title,
        w.date,
        w.level,
        w.price,
        w.location,
        fn_get_workshop_available_spots(w.workshop_id) AS available_spots
    FROM workshop w
    WHERE w.instructor_id = p_artist_id;
END //

DELIMITER ;

-- =============================================================
-- DEMO CALLS (commented out — uncomment to test)
-- =============================================================

-- SELECT fn_get_workshop_available_spots(1) AS spots_workshop_1;
-- SELECT fn_get_workshop_available_spots(3) AS spots_workshop_3;

-- SELECT fn_get_artist_average_rating(1) AS rating_elena;
-- SELECT fn_get_artist_average_rating(2) AS rating_marco;

-- CALL sp_generate_artist_report(1);  -- Elena Moreau's full report

-- CALL sp_create_workshop_with_artist(
--     'Intro to Watercolor', '2025-09-10 10:00:00', 120, 15, 55.00,
--     'Studio Central, Paris', 'Discover watercolor techniques.',
--     'Beginner', 'new.teacher@artmail.com', 'Jean Nouveau'
-- );

-- CALL sp_register_member_to_workshops(1, '2,5,6');
