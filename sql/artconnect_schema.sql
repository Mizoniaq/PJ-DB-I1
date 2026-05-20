-- =============================================================
-- ArtConnect Database Schema
-- Step 2 — Conceptual & Logical Modeling
-- Normalized to 3rd Normal Form (3NF)
-- =============================================================
DROP DATABASE IF EXISTS artconnect;
CREATE DATABASE artconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE artconnect;
-- =============================================================
-- LOOKUP TABLES (no dependencies)
-- =============================================================
CREATE TABLE discipline (
    discipline_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_discipline PRIMARY KEY (discipline_id),
    CONSTRAINT uq_discipline_name UNIQUE (name)
);
CREATE TABLE artwork_tag (
    tag_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_artwork_tag PRIMARY KEY (tag_id),
    CONSTRAINT uq_artwork_tag_name UNIQUE (name)
);
-- =============================================================
-- CORE ENTITIES
-- =============================================================
CREATE TABLE artist (
    artist_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    birth_year INT,
    contact_email VARCHAR(255),
    phone VARCHAR(50),
    city VARCHAR(100),
    website VARCHAR(255),
    social_media VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_artist PRIMARY KEY (artist_id),
    CONSTRAINT uq_artist_email UNIQUE (contact_email)
);
CREATE TABLE artwork (
    artwork_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    creation_year INT,
    type VARCHAR(100),
    medium VARCHAR(100),
    dimensions VARCHAR(100),
    description TEXT,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status ENUM('FOR_SALE', 'SOLD', 'EXHIBITED') NOT NULL DEFAULT 'FOR_SALE',
    artist_id INT NOT NULL,
    CONSTRAINT pk_artwork PRIMARY KEY (artwork_id),
    CONSTRAINT fk_artwork_artist FOREIGN KEY (artist_id) REFERENCES artist(artist_id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE gallery (
    gallery_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    owner_name VARCHAR(255),
    opening_hours VARCHAR(255),
    contact_phone VARCHAR(50),
    rating DECIMAL(3, 1),
    website VARCHAR(255),
    CONSTRAINT pk_gallery PRIMARY KEY (gallery_id)
);
CREATE TABLE exhibition (
    exhibition_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT,
    curator_name VARCHAR(255),
    theme VARCHAR(255),
    gallery_id INT NOT NULL,
    CONSTRAINT pk_exhibition PRIMARY KEY (exhibition_id),
    CONSTRAINT fk_exhibition_gallery FOREIGN KEY (gallery_id) REFERENCES gallery(gallery_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_exhibition_dates CHECK (end_date >= start_date)
);
CREATE TABLE workshop (
    workshop_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    date DATETIME NOT NULL,
    duration_minutes INT,
    max_participants INT,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    instructor_id INT NOT NULL,
    location VARCHAR(255),
    description TEXT,
    level VARCHAR(50),
    CONSTRAINT pk_workshop PRIMARY KEY (workshop_id),
    CONSTRAINT fk_workshop_instructor FOREIGN KEY (instructor_id) REFERENCES artist(artist_id) ON DELETE RESTRICT ON UPDATE CASCADE
);
CREATE TABLE community_member (
    member_id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    birth_year INT,
    phone VARCHAR(50),
    city VARCHAR(100),
    membership_type VARCHAR(50) NOT NULL DEFAULT 'free',
    CONSTRAINT pk_community_member PRIMARY KEY (member_id),
    CONSTRAINT uq_community_member_email UNIQUE (email),
    CONSTRAINT chk_membership_type CHECK (membership_type IN ('free', 'premium'))
);
CREATE TABLE booking (
    booking_id INT NOT NULL AUTO_INCREMENT,
    workshop_id INT NOT NULL,
    member_id INT NOT NULL,
    booking_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_booking PRIMARY KEY (booking_id),
    CONSTRAINT uq_booking_pair UNIQUE (workshop_id, member_id),
    CONSTRAINT fk_booking_workshop FOREIGN KEY (workshop_id) REFERENCES workshop(workshop_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_booking_member FOREIGN KEY (member_id) REFERENCES community_member(member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_payment_status CHECK (
        payment_status IN ('PENDING', 'PAID', 'CANCELLED')
    )
);
CREATE TABLE review (
    review_id INT NOT NULL AUTO_INCREMENT,
    reviewer_id INT NOT NULL,
    artwork_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    review_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT pk_review PRIMARY KEY (review_id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES community_member(member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_review_artwork FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_review_rating CHECK (
        rating BETWEEN 1 AND 5
    )
);
-- =============================================================
-- JUNCTION TABLES (Many-to-Many relationships)
-- =============================================================
-- Artist <-> Discipline (N:M)
CREATE TABLE artist_discipline (
    artist_id INT NOT NULL,
    discipline_id INT NOT NULL,
    CONSTRAINT pk_artist_discipline PRIMARY KEY (artist_id, discipline_id),
    CONSTRAINT fk_ad_artist FOREIGN KEY (artist_id) REFERENCES artist(artist_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ad_discipline FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id) ON DELETE CASCADE ON UPDATE CASCADE
);
-- CommunityMember <-> Discipline (N:M) — favorite disciplines
CREATE TABLE member_favorite_discipline (
    member_id INT NOT NULL,
    discipline_id INT NOT NULL,
    CONSTRAINT pk_member_fav_discipline PRIMARY KEY (member_id, discipline_id),
    CONSTRAINT fk_mfd_member FOREIGN KEY (member_id) REFERENCES community_member(member_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_mfd_discipline FOREIGN KEY (discipline_id) REFERENCES discipline(discipline_id) ON DELETE CASCADE ON UPDATE CASCADE
);
-- Exhibition <-> Artwork (N:M)
CREATE TABLE exhibition_artwork (
    exhibition_id INT NOT NULL,
    artwork_id INT NOT NULL,
    CONSTRAINT pk_exhibition_artwork PRIMARY KEY (exhibition_id, artwork_id),
    CONSTRAINT fk_ea_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibition(exhibition_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ea_artwork FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id) ON DELETE CASCADE ON UPDATE CASCADE
);
-- Artwork <-> ArtworkTag (N:M)
CREATE TABLE artwork_artwork_tag (
    artwork_id INT NOT NULL,
    tag_id INT NOT NULL,
    CONSTRAINT pk_artwork_tag_link PRIMARY KEY (artwork_id, tag_id),
    CONSTRAINT fk_aat_artwork FOREIGN KEY (artwork_id) REFERENCES artwork(artwork_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_aat_tag FOREIGN KEY (tag_id) REFERENCES artwork_tag(tag_id) ON DELETE CASCADE ON UPDATE CASCADE
);