package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation for ExhibitionDao.
 * Reconstructs Gallery and Artwork references.
 */
public class JdbcExhibitionDao implements ExhibitionDao {

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    @Override
    public List<Exhibition> findAll() {
        String sql = "SELECT e.exhibition_id, e.title, e.start_date, e.end_date, "
                + "e.description, e.curator_name, e.theme, "
                + "g.gallery_id, g.name AS gallery_name, g.address, g.rating "
                + "FROM exhibition e "
                + "JOIN gallery g ON e.gallery_id = g.gallery_id "
                + "ORDER BY e.exhibition_id";

        Map<Integer, Exhibition> exhibitionMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("exhibition_id");
                Exhibition ex = mapRow(rs);
                exhibitionMap.put(id, ex);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all exhibitions", e);
        }

        // Load artworks for each exhibition
        loadArtworksForExhibitions(exhibitionMap);

        return new ArrayList<>(exhibitionMap.values());
    }

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Override
    public void save(Exhibition exhibition) {
        String sql = "INSERT INTO exhibition (title, start_date, end_date, description, "
                + "curator_name, theme, gallery_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, "
                + "(SELECT gallery_id FROM gallery WHERE name = ? LIMIT 1))";

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int exhibitionId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, exhibition.getTitle());
                    ps.setDate(2, exhibition.getStartDate() != null ? Date.valueOf(exhibition.getStartDate()) : null);
                    ps.setDate(3, exhibition.getEndDate() != null ? Date.valueOf(exhibition.getEndDate()) : null);
                    ps.setString(4, exhibition.getDescription());
                    ps.setString(5, exhibition.getCuratorName());
                    ps.setString(6, exhibition.getTheme());
                    ps.setString(7, exhibition.getGallery() != null ? exhibition.getGallery().getName() : null);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            exhibitionId = keys.getInt(1);
                        } else {
                            throw new SQLException("Failed to get generated exhibition_id");
                        }
                    }
                }

                // Insert exhibition_artwork links
                if (exhibition.getArtworks() != null && !exhibition.getArtworks().isEmpty()) {
                    String linkSql = "INSERT INTO exhibition_artwork (exhibition_id, artwork_id) "
                            + "SELECT ?, artwork_id FROM artwork WHERE title = ?";
                    try (PreparedStatement ps2 = conn.prepareStatement(linkSql)) {
                        for (Artwork aw : exhibition.getArtworks()) {
                            ps2.setInt(1, exhibitionId);
                            ps2.setString(2, aw.getTitle());
                            ps2.addBatch();
                        }
                        ps2.executeBatch();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving exhibition", e);
        }
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    @Override
    public void update(Exhibition exhibition) {
        String sql = "UPDATE exhibition SET start_date = ?, end_date = ?, description = ?, "
                + "curator_name = ?, theme = ?, "
                + "gallery_id = (SELECT gallery_id FROM gallery WHERE name = ? LIMIT 1) "
                + "WHERE title = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, exhibition.getStartDate() != null ? Date.valueOf(exhibition.getStartDate()) : null);
            ps.setDate(2, exhibition.getEndDate() != null ? Date.valueOf(exhibition.getEndDate()) : null);
            ps.setString(3, exhibition.getDescription());
            ps.setString(4, exhibition.getCuratorName());
            ps.setString(5, exhibition.getTheme());
            ps.setString(6, exhibition.getGallery() != null ? exhibition.getGallery().getName() : null);
            ps.setString(7, exhibition.getTitle());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating exhibition", e);
        }
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Override
    public void delete(String title) {
        String sql = "DELETE FROM exhibition WHERE title = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting exhibition", e);
        }
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private Exhibition mapRow(ResultSet rs) throws SQLException {
        Gallery gallery = new Gallery();
        gallery.setName(rs.getString("gallery_name"));
        gallery.setAddress(rs.getString("address"));
        gallery.setRating(rs.getDouble("rating"));

        Exhibition ex = new Exhibition();
        ex.setTitle(rs.getString("title"));
        ex.setStartDate(rs.getDate("start_date").toLocalDate());
        ex.setEndDate(rs.getDate("end_date").toLocalDate());
        ex.setDescription(rs.getString("description"));
        ex.setCuratorName(rs.getString("curator_name"));
        ex.setTheme(rs.getString("theme"));
        ex.setGallery(gallery);
        return ex;
    }

    /**
     * Loads artworks for each exhibition via the exhibition_artwork junction table.
     */
    private void loadArtworksForExhibitions(Map<Integer, Exhibition> exhibitionMap) {
        if (exhibitionMap.isEmpty()) return;

        String sql = "SELECT ea.exhibition_id, aw.title AS artwork_title "
                + "FROM exhibition_artwork ea "
                + "JOIN artwork aw ON ea.artwork_id = aw.artwork_id "
                + "ORDER BY ea.exhibition_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int exId = rs.getInt("exhibition_id");
                Exhibition ex = exhibitionMap.get(exId);
                if (ex != null) {
                    Artwork aw = new Artwork();
                    aw.setTitle(rs.getString("artwork_title"));
                    ex.getArtworks().add(aw);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading exhibition artworks", e);
        }
    }
}
