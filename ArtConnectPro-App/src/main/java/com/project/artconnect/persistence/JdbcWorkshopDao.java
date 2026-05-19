package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation for WorkshopDao.
 * Reconstructs the Artist (instructor) reference.
 */
public class JdbcWorkshopDao implements WorkshopDao {

    @Override
    public Optional<Workshop> findById(Long id) {
        String sql = "SELECT w.workshop_id, w.title, w.date, w.duration_minutes, "
                + "w.max_participants, w.price, w.location, w.description, w.level, "
                + "a.artist_id, a.name AS instructor_name, a.bio, a.birth_year, "
                + "a.contact_email, a.city "
                + "FROM workshop w "
                + "JOIN artist a ON w.instructor_id = a.artist_id "
                + "WHERE w.workshop_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching workshop by id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Workshop> findAll() {
        String sql = "SELECT w.workshop_id, w.title, w.date, w.duration_minutes, "
                + "w.max_participants, w.price, w.location, w.description, w.level, "
                + "a.artist_id, a.name AS instructor_name, a.bio, a.birth_year, "
                + "a.contact_email, a.city "
                + "FROM workshop w "
                + "JOIN artist a ON w.instructor_id = a.artist_id "
                + "ORDER BY w.workshop_id";

        List<Workshop> workshops = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                workshops.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all workshops", e);
        }

        return workshops;
    }

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Override
    public void save(Workshop workshop) {
        String sql = "INSERT INTO workshop (title, date, duration_minutes, max_participants, "
                + "price, instructor_id, location, description, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (workshop.getInstructor() == null || workshop.getInstructor().getContactEmail() == null) {
            throw new IllegalArgumentException("Workshop must have an instructor with a valid email");
        }
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int instructorId = findArtistIdByEmail(conn, workshop.getInstructor().getContactEmail());
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, workshop.getTitle());
                    ps.setTimestamp(2, workshop.getDate() != null ? Timestamp.valueOf(workshop.getDate()) : null);
                    ps.setInt(3, workshop.getDurationMinutes());
                    ps.setInt(4, workshop.getMaxParticipants());
                    ps.setDouble(5, workshop.getPrice());
                    ps.setInt(6, instructorId);
                    ps.setString(7, workshop.getLocation());
                    ps.setString(8, workshop.getDescription());
                    ps.setString(9, workshop.getLevel());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving workshop", e);
        }
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    @Override
    public void update(Workshop workshop) {
        String sql = "UPDATE workshop SET date = ?, duration_minutes = ?, max_participants = ?, "
                + "price = ?, instructor_id = ?, location = ?, description = ?, level = ? WHERE title = ?";

        if (workshop.getInstructor() == null || workshop.getInstructor().getContactEmail() == null) {
            throw new IllegalArgumentException("Workshop must have an instructor with a valid email");
        }
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int instructorId = findArtistIdByEmail(conn, workshop.getInstructor().getContactEmail());
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setTimestamp(1, workshop.getDate() != null ? Timestamp.valueOf(workshop.getDate()) : null);
                    ps.setInt(2, workshop.getDurationMinutes());
                    ps.setInt(3, workshop.getMaxParticipants());
                    ps.setDouble(4, workshop.getPrice());
                    ps.setInt(5, instructorId);
                    ps.setString(6, workshop.getLocation());
                    ps.setString(7, workshop.getDescription());
                    ps.setString(8, workshop.getLevel());
                    ps.setString(9, workshop.getTitle());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error updating workshop", e);
        }
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Override
    public void delete(String title) {
        String sql = "DELETE FROM workshop WHERE title = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting workshop", e);
        }
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private int findArtistIdByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT artist_id FROM artist WHERE contact_email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("artist_id");
            }
        }
        throw new SQLException("Artist not found with email: " + email);
    }

    private Workshop mapRow(ResultSet rs) throws SQLException {
        Artist instructor = new Artist();
        instructor.setName(rs.getString("instructor_name"));
        instructor.setBio(rs.getString("bio"));
        instructor.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
        instructor.setContactEmail(rs.getString("contact_email"));
        instructor.setCity(rs.getString("city"));

        Workshop w = new Workshop();
        w.setTitle(rs.getString("title"));
        Timestamp ts = rs.getTimestamp("date");
        if (ts != null) {
            w.setDate(ts.toLocalDateTime());
        }
        w.setDurationMinutes(rs.getInt("duration_minutes"));
        w.setMaxParticipants(rs.getInt("max_participants"));
        w.setPrice(rs.getDouble("price"));
        w.setLocation(rs.getString("location"));
        w.setDescription(rs.getString("description"));
        w.setLevel(rs.getString("level"));
        w.setInstructor(instructor);
        return w;
    }
}
