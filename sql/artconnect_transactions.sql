-- =============================================================
-- ArtConnect — Transaction Test Scripts
-- Step 3, Task 4: Complex transactional scenarios demonstrating
--                  atomicity (COMMIT and ROLLBACK)
-- =============================================================
-- Prerequisites: Run all previous scripts (schema, data,
--                triggers, procedures)
-- =============================================================

USE artconnect;

DROP PROCEDURE IF EXISTS sp_demo_atomic_registration;

-- =============================================================
-- SCENARIO 1: Successful registration of a new premium member
-- =============================================================
-- A new member joins ArtConnect as premium, selects favorite
-- disciplines, books 3 workshops, and writes 2 reviews.
-- All operations must succeed atomically.
-- =============================================================

START TRANSACTION;

-- Step 1: Create the new member
INSERT INTO community_member (name, email, birth_year, phone, city, membership_type)
VALUES ('Léa Fontaine', 'lea.fontaine@email.com', 1994, '+33 6 55 44 33 22', 'Marseille', 'premium');

SET @new_member_id = LAST_INSERT_ID();

-- Step 2: Add favorite disciplines (Painting, Photography)
INSERT INTO member_favorite_discipline (member_id, discipline_id)
VALUES (@new_member_id, 1), (@new_member_id, 3);

-- Step 3: Book 3 workshops (1: Oil Painting, 3: Night Photo, 5: Geometric)
INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
VALUES (1, @new_member_id, NOW(), 'PAID');

INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
VALUES (3, @new_member_id, NOW(), 'PAID');

INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
VALUES (5, @new_member_id, NOW(), 'PENDING');

-- Step 4: Write 2 reviews on artworks
INSERT INTO review (reviewer_id, artwork_id, rating, comment, review_date)
VALUES (@new_member_id, 1, 5, 'Absolutely stunning. The light captures the essence of Montmartre.', CURDATE());

INSERT INTO review (reviewer_id, artwork_id, rating, comment, review_date)
VALUES (@new_member_id, 7, 4, 'Beautiful night photography. The neon reflections are mesmerizing.', CURDATE());

-- Everything succeeded → commit
COMMIT;

-- Verification: check the new member exists with all their data
SELECT 'SCENARIO 1 — Verification' AS test;

SELECT cm.name, cm.membership_type, cm.city
FROM community_member cm WHERE cm.email = 'lea.fontaine@email.com';

SELECT '  Bookings:' AS detail;
SELECT b.booking_id, w.title, b.payment_status
FROM booking b
JOIN workshop w ON b.workshop_id = w.workshop_id
WHERE b.member_id = @new_member_id;

SELECT '  Reviews:' AS detail;
SELECT r.review_id, aw.title, r.rating, r.comment
FROM review r
JOIN artwork aw ON r.artwork_id = aw.artwork_id
WHERE r.reviewer_id = @new_member_id;

SELECT '  Favorite disciplines:' AS detail;
SELECT d.name
FROM member_favorite_discipline mfd
JOIN discipline d ON mfd.discipline_id = d.discipline_id
WHERE mfd.member_id = @new_member_id;


-- =============================================================
-- SCENARIO 2: Failed registration — ROLLBACK due to full workshop
-- =============================================================
-- A member tries to register for multiple workshops, but one
-- of them is full. The entire transaction is rolled back to
-- maintain consistency: either ALL bookings succeed, or NONE.
-- =============================================================

-- First, let's artificially fill up workshop 2 (max = 8)
-- by temporarily reducing its capacity to match current bookings
SELECT 'SCENARIO 2 — Demonstrating ROLLBACK' AS test;

-- Save original max_participants for workshop 2
SET @original_max = (SELECT max_participants FROM workshop WHERE workshop_id = 2);

-- Set max_participants to current active booking count (making it full)
UPDATE workshop SET max_participants = (
    SELECT COUNT(*) FROM booking
    WHERE workshop_id = 2 AND payment_status IN ('PAID', 'PENDING')
) WHERE workshop_id = 2;

-- Now attempt a transaction that will fail
START TRANSACTION;

-- Step 1: Create another new member
INSERT INTO community_member (name, email, birth_year, phone, city, membership_type)
VALUES ('Pierre Durand', 'pierre.durand@email.com', 1990, '+33 7 11 22 33 44', 'Lille', 'free');

SET @failed_member_id = LAST_INSERT_ID();

-- Step 2: Book workshop 5 — should succeed
INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
VALUES (5, @failed_member_id, NOW(), 'PENDING');

-- Step 3: Book workshop 2 — should FAIL (full due to trigger)
-- This will raise an error. In a real application, the error
-- handler would execute ROLLBACK. Here we demonstrate manually.
-- 
-- NOTE: In MySQL, if the trigger raises SIGNAL, the INSERT fails
-- but the transaction is NOT automatically rolled back.
-- In production code, you would wrap this in a DECLARE HANDLER.
-- For demonstration, we rollback manually after the error.

-- Uncomment the next line to see the trigger error:
-- INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
-- VALUES (2, @failed_member_id, NOW(), 'PENDING');

-- Since we can't continue after a SIGNAL in a script without
-- a handler, let's simulate the detection and rollback:
ROLLBACK;

-- Restore the original capacity
UPDATE workshop SET max_participants = @original_max WHERE workshop_id = 2;

-- Verification: Pierre Durand should NOT exist (rolled back)
SELECT 'SCENARIO 2 — Verification (should be empty):' AS test;
SELECT * FROM community_member WHERE email = 'pierre.durand@email.com';


-- =============================================================
-- SCENARIO 2b: ROLLBACK with stored procedure and handler
-- =============================================================
-- This version uses a stored procedure with proper error
-- handling to demonstrate the full ROLLBACK flow.
-- =============================================================

DELIMITER //

CREATE PROCEDURE sp_demo_atomic_registration()
BEGIN
    DECLARE v_member_id INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SELECT 'SCENARIO 2b — Transaction ROLLED BACK due to error.' AS result;
    END;

    -- Temporarily fill workshop 2
    UPDATE workshop SET max_participants = (
        SELECT COUNT(*) FROM booking
        WHERE workshop_id = 2 AND payment_status IN ('PAID', 'PENDING')
    ) WHERE workshop_id = 2;

    START TRANSACTION;

    -- Create member
    INSERT INTO community_member (name, email, birth_year, phone, city, membership_type)
    VALUES ('Marc Lefèvre', 'marc.lefevre@email.com', 1987, '+33 6 99 88 77 66', 'Toulouse', 'free');

    SET v_member_id = LAST_INSERT_ID();

    -- Book workshop 5 — OK
    INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
    VALUES (5, v_member_id, NOW(), 'PENDING');

    -- Book workshop 2 — FAILS (trigger: full)
    INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
    VALUES (2, v_member_id, NOW(), 'PENDING');

    -- If we reach here, all succeeded
    COMMIT;
    SELECT 'SCENARIO 2b — Transaction COMMITTED successfully.' AS result;
END //

DELIMITER ;

-- Run the demo
CALL sp_demo_atomic_registration();

-- Restore capacity
UPDATE workshop SET max_participants = 8 WHERE workshop_id = 2;

-- Verification: Marc should NOT exist
SELECT 'SCENARIO 2b — Verification (should be empty):' AS test;
SELECT * FROM community_member WHERE email = 'marc.lefevre@email.com';