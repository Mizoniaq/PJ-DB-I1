-- =============================================================
-- ArtConnect — Sample Data Insertion Script
-- Step 3, Task 1: Populate the database with realistic test data
-- =============================================================
-- Prerequisites: Run artconnect_schema.sql first.
-- =============================================================

USE artconnect;

-- =============================================================
-- 1. LOOKUP TABLES
-- =============================================================

-- Disciplines (5)
INSERT INTO discipline (name) VALUES
    ('Painting'),
    ('Sculpture'),
    ('Photography'),
    ('Digital Art'),
    ('Mixed Media');

-- Artwork Tags (8)
INSERT INTO artwork_tag (name) VALUES
    ('Abstract'),
    ('Contemporary'),
    ('Portrait'),
    ('Landscape'),
    ('Minimalist'),
    ('Surrealist'),
    ('Pop Art'),
    ('Expressionist');

-- =============================================================
-- 2. ARTISTS (6)
-- =============================================================

INSERT INTO artist (name, bio, birth_year, contact_email, phone, city, website, social_media, is_active) VALUES
    ('Elena Moreau',
     'French painter known for her vibrant abstract landscapes blending impressionism with modern techniques.',
     1985, 'elena.moreau@artmail.com', '+33 6 12 34 56 78', 'Paris',
     'https://elenamoreau.art', '@elenamoreau_art', TRUE),

    ('Marco Bellini',
     'Italian sculptor specializing in large-scale bronze and marble installations inspired by Renaissance masters.',
     1978, 'marco.bellini@artmail.com', '+39 06 1234 5678', 'Rome',
     'https://marcobellini.it', '@bellini_sculpt', TRUE),

    ('Yuki Tanaka',
     'Japanese digital artist and photographer exploring the intersection of technology, nature, and urban life.',
     1992, 'yuki.tanaka@artmail.com', '+81 3 1234 5678', 'Tokyo',
     'https://yukitanaka.jp', '@yuki_creates', TRUE),

    ('Amara Diallo',
     'Senegalese mixed-media artist whose work explores identity, migration, and cultural heritage through bold collages.',
     1990, 'amara.diallo@artmail.com', '+221 77 123 45 67', 'Dakar',
     'https://amaradiallo.art', '@amara_art', TRUE),

    ('Lucas Engström',
     'Swedish minimalist painter focused on geometric abstraction and color theory.',
     1982, 'lucas.engstrom@artmail.com', '+46 70 123 45 67', 'Stockholm',
     NULL, '@engstrom_minimal', TRUE),

    ('Sophie Dubois',
     'Belgian photographer and former gallery owner. Currently on a creative sabbatical.',
     1975, 'sophie.dubois@artmail.com', '+32 2 123 45 67', 'Brussels',
     'https://sophiedubois.be', '@sophie_lens', FALSE);

-- =============================================================
-- 3. ARTIST ↔ DISCIPLINE (many-to-many)
-- =============================================================

-- Elena Moreau: Painting, Mixed Media
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
    (1, 1), (1, 5);

-- Marco Bellini: Sculpture
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
    (2, 2);

-- Yuki Tanaka: Photography, Digital Art
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
    (3, 3), (3, 4);

-- Amara Diallo: Mixed Media, Painting
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
    (4, 5), (4, 1);

-- Lucas Engström: Painting
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
    (5, 1);

-- Sophie Dubois: Photography
INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
    (6, 3);

-- =============================================================
-- 4. ARTWORKS (15)
-- =============================================================

-- Elena Moreau's works (3)
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
    ('Sunrise over Montmartre', 2023, 'Painting', 'Oil on canvas', '120x80 cm',
     'A vibrant depiction of dawn breaking over the Montmartre rooftops with bold orange and pink hues.',
     4500.00, 'FOR_SALE', 1),
    ('Lavender Fields Forever', 2022, 'Painting', 'Acrylic on canvas', '100x70 cm',
     'An impressionistic view of Provence lavender fields under a summer sky.',
     3200.00, 'SOLD', 1),
    ('Urban Fragments #7', 2024, 'Mixed Media', 'Collage and acrylic', '90x60 cm',
     'Part of a series exploring the textures and rhythms of city life through layered materials.',
     2800.00, 'EXHIBITED', 1);

-- Marco Bellini's works (3)
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
    ('The Thinker Reimagined', 2021, 'Sculpture', 'Bronze', '180x60x60 cm',
     'A modern reinterpretation of Rodin''s famous work, featuring angular geometric forms.',
     15000.00, 'EXHIBITED', 2),
    ('Harmony in Stone', 2023, 'Sculpture', 'Carrara marble', '90x40x40 cm',
     'An abstract marble piece evoking balance and serenity through flowing curves.',
     8500.00, 'FOR_SALE', 2),
    ('Iron Winds', 2024, 'Sculpture', 'Welded iron', '200x150x100 cm',
     'A large-scale outdoor installation capturing the movement of wind through twisted metal forms.',
     22000.00, 'FOR_SALE', 2);

-- Yuki Tanaka's works (3)
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
    ('Neon Dreams: Shibuya', 2024, 'Photography', 'Digital print on aluminum', '150x100 cm',
     'A long-exposure photograph of Shibuya crossing at night, capturing streams of light and movement.',
     1800.00, 'FOR_SALE', 3),
    ('Digital Garden #3', 2023, 'Digital Art', 'Generative algorithm, printed on canvas', '80x80 cm',
     'An algorithmically generated botanical composition blending organic and digital forms.',
     2200.00, 'SOLD', 3),
    ('Tokyo Solitude', 2022, 'Photography', 'Fine art print', '60x40 cm',
     'A quiet, contemplative black-and-white street photograph from a rainy evening in Shinjuku.',
     1200.00, 'FOR_SALE', 3);

-- Amara Diallo's works (3)
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
    ('Roots and Routes', 2023, 'Mixed Media', 'Fabric, acrylic, found objects', '140x100 cm',
     'A large-scale collage exploring themes of African diaspora and cultural identity.',
     5500.00, 'EXHIBITED', 4),
    ('Market Day in Dakar', 2024, 'Painting', 'Oil on canvas', '100x80 cm',
     'A colorful, energetic depiction of the Sandaga market scene.',
     3800.00, 'FOR_SALE', 4),
    ('Woven Memories', 2022, 'Mixed Media', 'Textile and ink on paper', '70x50 cm',
     'An intimate piece combining traditional West African textiles with calligraphic ink drawings.',
     2400.00, 'SOLD', 4);

-- Lucas Engström's works (2)
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
    ('Composition in Blue #12', 2024, 'Painting', 'Acrylic on panel', '60x60 cm',
     'A geometric abstraction in various shades of blue exploring depth and symmetry.',
     1900.00, 'FOR_SALE', 5),
    ('Silent Grid', 2023, 'Painting', 'Oil on linen', '80x80 cm',
     'A meditative grid pattern in muted tones, inspired by Scandinavian design principles.',
     2100.00, 'FOR_SALE', 5);

-- Sophie Dubois' work (1)
INSERT INTO artwork (title, creation_year, type, medium, dimensions, description, price, status, artist_id) VALUES
    ('Ghent in the Mist', 2019, 'Photography', 'Silver gelatin print', '50x40 cm',
     'An atmospheric photograph of the medieval architecture of Ghent on a foggy morning.',
     950.00, 'SOLD', 6);

-- =============================================================
-- 5. ARTWORK ↔ TAG (many-to-many)
-- =============================================================

-- artwork_id 1: Sunrise over Montmartre → Landscape, Expressionist
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (1, 4), (1, 8);
-- artwork_id 2: Lavender Fields → Landscape, Contemporary
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (2, 4), (2, 2);
-- artwork_id 3: Urban Fragments → Abstract, Contemporary
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (3, 1), (3, 2);
-- artwork_id 4: Thinker Reimagined → Contemporary
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (4, 2);
-- artwork_id 5: Harmony in Stone → Abstract, Minimalist
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (5, 1), (5, 5);
-- artwork_id 6: Iron Winds → Abstract, Contemporary
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (6, 1), (6, 2);
-- artwork_id 7: Neon Dreams → Contemporary
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (7, 2);
-- artwork_id 8: Digital Garden → Abstract, Surrealist
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (8, 1), (8, 6);
-- artwork_id 9: Tokyo Solitude → Minimalist, Portrait
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (9, 5), (9, 3);
-- artwork_id 10: Roots and Routes → Contemporary, Expressionist
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (10, 2), (10, 8);
-- artwork_id 11: Market Day in Dakar → Expressionist, Pop Art
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (11, 8), (11, 7);
-- artwork_id 12: Woven Memories → Abstract, Contemporary
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (12, 1), (12, 2);
-- artwork_id 13: Composition in Blue → Abstract, Minimalist
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (13, 1), (13, 5);
-- artwork_id 14: Silent Grid → Minimalist
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (14, 5);
-- artwork_id 15: Ghent in the Mist → Landscape
INSERT INTO artwork_artwork_tag (artwork_id, tag_id) VALUES (15, 4);

-- =============================================================
-- 6. GALLERIES (4)
-- =============================================================

INSERT INTO gallery (name, address, owner_name, opening_hours, contact_phone, rating, website) VALUES
    ('Galerie Lumière', '15 Rue de Rivoli, 75001 Paris, France', 'Isabelle Fontaine',
     'Tue-Sun 10:00-19:00', '+33 1 42 60 12 34', 4.7, 'https://galerielumiere.fr'),
    ('Spazio Arte Moderna', 'Via del Corso 18, 00186 Roma, Italy', 'Giovanni Rossi',
     'Mon-Sat 11:00-20:00', '+39 06 6789 0123', 4.3, 'https://spazioartemoderna.it'),
    ('Nordic Art House', 'Drottninggatan 45, 111 21 Stockholm, Sweden', 'Anna Lindqvist',
     'Wed-Sun 12:00-18:00', '+46 8 123 456 78', 4.5, 'https://nordicarthouse.se'),
    ('Gallery Horizons', '22 King Street, Covent Garden, London WC2E 8JD, UK', 'James Wright',
     'Mon-Sun 10:00-21:00', '+44 20 7946 0958', 4.8, 'https://galleryhorizons.co.uk');

-- =============================================================
-- 7. EXHIBITIONS (5)
-- =============================================================

INSERT INTO exhibition (title, start_date, end_date, description, curator_name, theme, gallery_id) VALUES
    ('Visions of Tomorrow', '2024-09-01', '2024-12-15',
     'An exhibition exploring how contemporary artists envision the future through various media.',
     'Isabelle Fontaine', 'Futurism & Innovation', 1),
    ('Sculpting the Modern World', '2024-10-15', '2025-03-30',
     'A curated showcase of modern sculptural works that challenge perceptions of form and space.',
     'Giovanni Rossi', 'Modern Sculpture', 2),
    ('Light & Shadow: Nordic Perspectives', '2025-01-10', '2025-05-20',
     'A celebration of Nordic artistic vision featuring minimalist and contemporary Scandinavian art.',
     'Anna Lindqvist', 'Nordic Minimalism', 3),
    ('Global Threads', '2025-02-01', '2025-06-30',
     'An international exhibition weaving together diverse cultural narratives through mixed media.',
     'James Wright', 'Cultural Identity', 4),
    ('Urban Pulse', '2025-06-01', '2025-09-15',
     'A dynamic exhibition capturing the energy and rhythm of city life across the globe.',
     'Isabelle Fontaine', 'Urban Life', 1);

-- =============================================================
-- 8. EXHIBITION ↔ ARTWORK (many-to-many, cross-participations)
-- =============================================================

-- Exhibition 1 (Visions of Tomorrow): artworks 3, 7, 8
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
    (1, 3), (1, 7), (1, 8);
-- Exhibition 2 (Sculpting the Modern World): artworks 4, 5, 6
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
    (2, 4), (2, 5), (2, 6);
-- Exhibition 3 (Nordic Perspectives): artworks 13, 14, 9
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
    (3, 13), (3, 14), (3, 9);
-- Exhibition 4 (Global Threads): artworks 10, 12, 3  ← artwork 3 in TWO exhibitions (cross-participation!)
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
    (4, 10), (4, 12), (4, 3);
-- Exhibition 5 (Urban Pulse): artworks 1, 7, 11  ← artwork 7 in TWO exhibitions (cross-participation!)
INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
    (5, 1), (5, 7), (5, 11);

-- =============================================================
-- 9. WORKSHOPS (6)
-- =============================================================

INSERT INTO workshop (title, date, duration_minutes, max_participants, price, instructor_id, location, description, level) VALUES
    ('Introduction to Oil Painting', '2025-03-15 10:00:00', 180, 12, 75.00, 1,
     'Galerie Lumière, Paris',
     'Learn the fundamentals of oil painting: color mixing, brushwork, and composition.',
     'Beginner'),
    ('Abstract Sculpture Workshop', '2025-04-10 14:00:00', 240, 8, 120.00, 2,
     'Spazio Arte Moderna, Rome',
     'Hands-on workshop exploring abstract forms using clay and wire. All materials provided.',
     'Intermediate'),
    ('Night Photography Masterclass', '2025-05-20 19:00:00', 150, 15, 60.00, 3,
     'Nordic Art House, Stockholm',
     'Capture the magic of the city at night. Bring your camera (DSLR or mirrorless recommended).',
     'Intermediate'),
    ('Mixed Media Collage Intensive', '2025-06-05 09:30:00', 300, 10, 95.00, 4,
     'Gallery Horizons, London',
     'A full-day intensive on creating mixed-media collages incorporating fabric, paper, and found objects.',
     'Advanced'),
    ('Geometric Abstraction: Theory & Practice', '2025-07-12 11:00:00', 120, 20, 45.00, 5,
     'Nordic Art House, Stockholm',
     'Explore the principles of geometric abstraction and create your own minimalist composition.',
     'Beginner'),
    ('Advanced Color Theory for Artists', '2025-08-22 13:00:00', 180, 10, 85.00, 1,
     'Galerie Lumière, Paris',
     'A deep dive into color theory: complementary palettes, value scales, and emotional impact of color.',
     'Advanced');

-- =============================================================
-- 10. COMMUNITY MEMBERS (8)
-- =============================================================

INSERT INTO community_member (name, email, birth_year, phone, city, membership_type) VALUES
    ('Alice Martin', 'alice.martin@email.com', 1995, '+33 6 98 76 54 32', 'Paris', 'premium'),
    ('Ben Thompson', 'ben.thompson@email.com', 1988, '+44 20 7123 4567', 'London', 'free'),
    ('Carla Ruiz', 'carla.ruiz@email.com', 1993, '+34 91 234 5678', 'Madrid', 'premium'),
    ('David Chen', 'david.chen@email.com', 2000, '+46 70 987 65 43', 'Stockholm', 'free'),
    ('Emma Johansson', 'emma.johansson@email.com', 1997, '+46 73 456 78 90', 'Stockholm', 'premium'),
    ('Fatou Ndiaye', 'fatou.ndiaye@email.com', 1991, '+221 78 654 32 10', 'Dakar', 'free'),
    ('Guillaume Petit', 'guillaume.petit@email.com', 1986, '+33 7 12 34 56 78', 'Lyon', 'free'),
    ('Hannah Schmidt', 'hannah.schmidt@email.com', 1999, '+49 30 1234 5678', 'Berlin', 'premium');

-- =============================================================
-- 11. MEMBER ↔ DISCIPLINE (favorite disciplines)
-- =============================================================

INSERT INTO member_favorite_discipline (member_id, discipline_id) VALUES
    (1, 1), (1, 5),      -- Alice: Painting, Mixed Media
    (2, 2), (2, 3),      -- Ben: Sculpture, Photography
    (3, 1), (3, 4),      -- Carla: Painting, Digital Art
    (4, 3),              -- David: Photography
    (5, 1), (5, 2),      -- Emma: Painting, Sculpture
    (6, 5), (6, 1),      -- Fatou: Mixed Media, Painting
    (7, 3), (7, 4),      -- Guillaume: Photography, Digital Art
    (8, 4), (8, 2);      -- Hannah: Digital Art, Sculpture

-- =============================================================
-- 12. BOOKINGS (12) — various payment statuses
-- =============================================================

INSERT INTO booking (workshop_id, member_id, booking_date, payment_status) VALUES
    -- Workshop 1 (Oil Painting, Paris): Alice, Carla, Guillaume
    (1, 1, '2025-02-20 14:30:00', 'PAID'),
    (1, 3, '2025-02-22 09:15:00', 'PAID'),
    (1, 7, '2025-02-25 16:00:00', 'PENDING'),
    -- Workshop 2 (Sculpture, Rome): Ben, Emma
    (2, 2, '2025-03-01 11:00:00', 'PAID'),
    (2, 5, '2025-03-05 10:30:00', 'CANCELLED'),
    -- Workshop 3 (Night Photography, Stockholm): David, Emma, Guillaume, Hannah
    (3, 4, '2025-04-10 08:00:00', 'PAID'),
    (3, 5, '2025-04-12 18:45:00', 'PAID'),
    (3, 7, '2025-04-15 12:00:00', 'PAID'),
    (3, 8, '2025-04-18 09:00:00', 'PENDING'),
    -- Workshop 4 (Mixed Media, London): Ben, Fatou, Hannah
    (4, 2, '2025-05-01 13:00:00', 'PAID'),
    (4, 6, '2025-05-03 15:30:00', 'PAID'),
    (4, 8, '2025-05-05 10:00:00', 'CANCELLED');

-- =============================================================
-- 13. REVIEWS (10) — various ratings
-- =============================================================

INSERT INTO review (reviewer_id, artwork_id, rating, comment, review_date) VALUES
    (1, 1, 5, 'Absolutely breathtaking! The colors feel alive.', '2024-10-15'),
    (1, 10, 4, 'Very powerful message. The textures are fascinating.', '2025-03-10'),
    (2, 4, 5, 'A masterpiece. The geometric reinterpretation is brilliant.', '2024-11-20'),
    (2, 6, 4, 'Impressive scale and craftsmanship. Would love to see it outdoors.', '2025-01-08'),
    (3, 7, 5, 'Stunning long exposure. Captures the essence of Shibuya perfectly.', '2024-12-05'),
    (4, 13, 3, 'Interesting concept but feels a bit cold. The blue tones are nice though.', '2025-02-14'),
    (4, 14, 4, 'Very meditative. It grows on you the longer you look.', '2025-02-14'),
    (5, 1, 4, 'Beautiful use of light. Reminds me of early Monet.', '2025-01-20'),
    (6, 11, 5, 'I can feel the energy of Dakar in every brushstroke! Amazing.', '2025-04-01'),
    (8, 8, 4, 'Fascinating blend of organic and digital. Very original approach.', '2025-03-22');
