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
    // HELPERS
    // ---------------------------------------------------------------

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
