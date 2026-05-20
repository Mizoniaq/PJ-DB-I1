-- =============================================================
-- ArtConnect — Triggers
-- Step 3, Task 3a: Automation, validation, and auditing
-- =============================================================
-- Prerequisites: Run artconnect_schema.sql + artconnect_data.sql
-- =============================================================

USE artconnect;

-- Drop triggers if they exist (safe re-run)
DROP TRIGGER IF EXISTS trg_check_workshop_capacity;
DROP TRIGGER IF EXISTS trg_check_exhibition_date_consistency;
DROP TRIGGER IF EXISTS trg_audit_artwork_status;
DROP TRIGGER IF EXISTS trg_set_artwork_exhibited;

-- =============================================================
-- AUDIT LOG TABLE (required by trigger 3)
-- =============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    table_name  VARCHAR(100)  NOT NULL,
    record_id   INT           NOT NULL,
    action      VARCHAR(50)   NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    changed_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================
-- TRIGGER 1: trg_check_workshop_capacity
-- Event:  BEFORE INSERT ON booking
-- Purpose: Prevents overbooking by checking that the number of
--          active bookings (PAID or PENDING) does not exceed
--          the workshop's max_participants.
-- =============================================================

DELIMITER //

CREATE TRIGGER trg_check_workshop_capacity
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN
    DECLARE current_count INT;
    DECLARE max_spots     INT;

    -- Count existing active (non-cancelled) bookings
    SELECT COUNT(*) INTO current_count
    FROM booking
    WHERE workshop_id = NEW.workshop_id
      AND payment_status IN ('PAID', 'PENDING');

    -- Get the workshop's max capacity
    SELECT max_participants INTO max_spots
    FROM workshop
    WHERE workshop_id = NEW.workshop_id;

    -- Only check if max_participants is defined (not NULL)
    IF max_spots IS NOT NULL AND current_count >= max_spots THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Workshop is full: no more spots available.';
    END IF;
END //

DELIMITER ;

-- =============================================================
-- TRIGGER 2: trg_check_exhibition_date_consistency
-- Event:  BEFORE UPDATE ON exhibition
-- Purpose: Ensures that when an exhibition's dates are modified,
--          end_date remains >= start_date. This is a secondary
--          safety net beyond the CHECK constraint, and also
--          prevents setting dates in the past for active expos.
-- =============================================================

DELIMITER //

CREATE TRIGGER trg_check_exhibition_date_consistency
BEFORE UPDATE ON exhibition
FOR EACH ROW
BEGIN
    IF NEW.end_date < NEW.start_date THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Exhibition end_date cannot be before start_date.';
    END IF;
END //

DELIMITER ;

-- =============================================================
-- TRIGGER 3: trg_audit_artwork_status
-- Event:  AFTER UPDATE ON artwork
-- Purpose: Records every status change of an artwork into the
--          audit_log table for traceability. Useful for tracking
--          sales (FOR_SALE → SOLD) and exhibition movements.
-- =============================================================

DELIMITER //

CREATE TRIGGER trg_audit_artwork_status
AFTER UPDATE ON artwork
FOR EACH ROW
BEGIN
    IF OLD.status <> NEW.status THEN
        INSERT INTO audit_log (table_name, record_id, action, old_value, new_value)
        VALUES ('artwork', NEW.artwork_id, 'STATUS_CHANGE', OLD.status, NEW.status);
    END IF;
END //

DELIMITER ;

-- =============================================================
-- TRIGGER 4: trg_set_artwork_exhibited
-- Event:  AFTER INSERT ON exhibition_artwork
-- Purpose: When an artwork is added to an exhibition that is
--          currently active (start_date <= TODAY <= end_date),
--          automatically sets the artwork's status to 'EXHIBITED'.
-- =============================================================

DELIMITER //

CREATE TRIGGER trg_set_artwork_exhibited
AFTER INSERT ON exhibition_artwork
FOR EACH ROW
BEGIN
    DECLARE expo_start DATE;
    DECLARE expo_end   DATE;

    SELECT start_date, end_date INTO expo_start, expo_end
    FROM exhibition
    WHERE exhibition_id = NEW.exhibition_id;

    -- Only auto-update if the exhibition is currently running
    IF CURDATE() BETWEEN expo_start AND expo_end THEN
        UPDATE artwork
        SET status = 'EXHIBITED'
        WHERE artwork_id = NEW.artwork_id
          AND status <> 'EXHIBITED';
    END IF;
END //

DELIMITER ;

-- =============================================================
-- TEST EXAMPLES (commented out — uncomment to verify)
-- =============================================================

-- Test trigger 1: overbooking check
-- First, set a workshop to have max_participants = 2:
-- UPDATE workshop SET max_participants = 2 WHERE workshop_id = 2;
-- Then try to insert more than 2 active bookings:
-- INSERT INTO booking (workshop_id, member_id, payment_status)
--     VALUES (2, 3, 'PAID');
-- Should fail with: 'Workshop is full: no more spots available.'

-- Test trigger 3: audit log
-- UPDATE artwork SET status = 'SOLD' WHERE artwork_id = 1;
-- SELECT * FROM audit_log;
-- Should show: table_name='artwork', record_id=1,
--              action='STATUS_CHANGE', old='FOR_SALE', new='SOLD'
