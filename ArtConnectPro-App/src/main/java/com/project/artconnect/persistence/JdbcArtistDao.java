package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation for ArtistDao.
 * Uses PreparedStatements and try-with-resources for safe resource handling.
 */
public class JdbcArtistDao implements ArtistDao {

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    @Override
    public List<Artist> findAll() {
        String sql = "SELECT a.artist_id, a.name, a.bio, a.birth_year, a.contact_email, "
                + "a.phone, a.city, a.website, a.social_media, a.is_active "
                + "FROM artist a ORDER BY a.artist_id";

        Map<Integer, Artist> artistMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("artist_id");
                Artist artist = mapRow(rs);
                artistMap.put(id, artist);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all artists", e);
        }

        // Load disciplines for each artist
        loadDisciplinesForArtists(artistMap);

        return new ArrayList<>(artistMap.values());
    }

    @Override
    public List<Artist> findByCity(String city) {
        String sql = "SELECT a.artist_id, a.name, a.bio, a.birth_year, a.contact_email, "
                + "a.phone, a.city, a.website, a.social_media, a.is_active "
                + "FROM artist a WHERE a.city = ? ORDER BY a.artist_id";

        Map<Integer, Artist> artistMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, city);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("artist_id");
                    artistMap.put(id, mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching artists by city", e);
        }

        loadDisciplinesForArtists(artistMap);
        return new ArrayList<>(artistMap.values());
    }

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Override
    public void save(Artist artist) {
        String insertArtist = "INSERT INTO artist (name, bio, birth_year, contact_email, phone, "
                + "city, website, social_media, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int artistId;
                try (PreparedStatement ps = conn.prepareStatement(insertArtist, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, artist.getName());
                    ps.setString(2, artist.getBio());
                    ps.setObject(3, artist.getBirthYear(), Types.INTEGER);
                    ps.setString(4, artist.getContactEmail());
                    ps.setString(5, artist.getPhone());
                    ps.setString(6, artist.getCity());
                    ps.setString(7, artist.getWebsite());
                    ps.setString(8, artist.getSocialMedia());
                    ps.setBoolean(9, artist.isActive());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            artistId = keys.getInt(1);
                            artist.setArtistId(artistId);
                        } else {
                            throw new SQLException("Failed to get generated artist_id");
                        }
                    }
                }

                // Insert disciplines
                insertDisciplines(conn, artistId, artist.getDisciplines());

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving artist", e);
        }
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    @Override
    public void update(Artist artist) {
        String updateSql = "UPDATE artist SET name = ?, bio = ?, birth_year = ?, contact_email = ?, "
                + "phone = ?, city = ?, website = ?, social_media = ?, is_active = ? "
                + "WHERE artist_id = ?";

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update artist row
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, artist.getName());
                    ps.setString(2, artist.getBio());
                    ps.setObject(3, artist.getBirthYear(), Types.INTEGER);
                    ps.setString(4, artist.getContactEmail());
                    ps.setString(5, artist.getPhone());
                    ps.setString(6, artist.getCity());
                    ps.setString(7, artist.getWebsite());
                    ps.setString(8, artist.getSocialMedia());
                    ps.setBoolean(9, artist.isActive());
                    ps.setInt(10, artist.getArtistId());
                    ps.executeUpdate();
                }

                int artistId = artist.getArtistId();

                // Delete old disciplines, insert new ones
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM artist_discipline WHERE artist_id = ?")) {
                    del.setInt(1, artistId);
                    del.executeUpdate();
                }
                insertDisciplines(conn, artistId, artist.getDisciplines());

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating artist", e);
        }
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Override
    public void delete(String artistName) {
        String sql = "DELETE FROM artist WHERE name = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artistName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting artist", e);
        }
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    /**
     * Maps a ResultSet row to an Artist object (without disciplines).
     */
    private Artist mapRow(ResultSet rs) throws SQLException {
        Artist a = new Artist();
        a.setArtistId(rs.getInt("artist_id"));
        a.setName(rs.getString("name"));
        a.setBio(rs.getString("bio"));
        a.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
        a.setContactEmail(rs.getString("contact_email"));
        a.setPhone(rs.getString("phone"));
        a.setCity(rs.getString("city"));
        a.setWebsite(rs.getString("website"));
        a.setSocialMedia(rs.getString("social_media"));
        a.setActive(rs.getBoolean("is_active"));
        return a;
    }

    /**
     * Loads disciplines for a batch of artists in a single query.
     */
    private void loadDisciplinesForArtists(Map<Integer, Artist> artistMap) {
        if (artistMap.isEmpty()) return;

        String sql = "SELECT ad.artist_id, d.name "
                + "FROM artist_discipline ad "
                + "JOIN discipline d ON ad.discipline_id = d.discipline_id "
                + "ORDER BY ad.artist_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int artistId = rs.getInt("artist_id");
                Artist artist = artistMap.get(artistId);
                if (artist != null) {
                    artist.getDisciplines().add(new Discipline(rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading artist disciplines", e);
        }
    }

    /**
     * Inserts discipline links for a given artist_id.
     */
    private void insertDisciplines(Connection conn, int artistId, List<Discipline> disciplines)
            throws SQLException {
        if (disciplines == null || disciplines.isEmpty()) return;

        String sql = "INSERT INTO artist_discipline (artist_id, discipline_id) "
                + "SELECT ?, discipline_id FROM discipline WHERE name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Discipline d : disciplines) {
                ps.setInt(1, artistId);
                ps.setString(2, d.getName());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Resolves artist_id from the artist name.
     */
    private int findArtistIdByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT artist_id FROM artist WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("artist_id");
                }
            }
        }
        throw new SQLException("Artist not found: " + name);
    }
}
