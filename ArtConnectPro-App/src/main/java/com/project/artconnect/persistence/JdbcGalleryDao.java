package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation for GalleryDao.
 * Also loads associated exhibitions.
 */
public class JdbcGalleryDao implements GalleryDao {

    @Override
    public Optional<Gallery> findById(Long id) {
        String sql = "SELECT gallery_id, name, address, owner_name, opening_hours, "
                + "contact_phone, rating, website "
                + "FROM gallery WHERE gallery_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Gallery g = mapRow(rs);
                    loadExhibitions(conn, rs.getInt("gallery_id"), g);
                    return Optional.of(g);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching gallery by id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Gallery> findAll() {
        String sql = "SELECT gallery_id, name, address, owner_name, opening_hours, "
                + "contact_phone, rating, website "
                + "FROM gallery ORDER BY gallery_id";

        Map<Integer, Gallery> galleryMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int gid = rs.getInt("gallery_id");
                Gallery g = mapRow(rs);
                galleryMap.put(gid, g);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all galleries", e);
        }

        // Load exhibitions for each gallery
        loadExhibitionsForGalleries(galleryMap);

        return new ArrayList<>(galleryMap.values());
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private Gallery mapRow(ResultSet rs) throws SQLException {
        Gallery g = new Gallery();
        g.setName(rs.getString("name"));
        g.setAddress(rs.getString("address"));
        g.setOwnerName(rs.getString("owner_name"));
        g.setOpeningHours(rs.getString("opening_hours"));
        g.setContactPhone(rs.getString("contact_phone"));
        g.setRating(rs.getDouble("rating"));
        g.setWebsite(rs.getString("website"));
        return g;
    }

    private void loadExhibitions(Connection conn, int galleryId, Gallery gallery) throws SQLException {
        String sql = "SELECT exhibition_id, title, start_date, end_date, "
                + "description, curator_name, theme "
                + "FROM exhibition WHERE gallery_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, galleryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Exhibition ex = new Exhibition();
                    ex.setTitle(rs.getString("title"));
                    ex.setStartDate(rs.getDate("start_date").toLocalDate());
                    ex.setEndDate(rs.getDate("end_date").toLocalDate());
                    ex.setDescription(rs.getString("description"));
                    ex.setCuratorName(rs.getString("curator_name"));
                    ex.setTheme(rs.getString("theme"));
                    ex.setGallery(gallery);
                    gallery.getExhibitions().add(ex);
                }
            }
        }
    }

    private void loadExhibitionsForGalleries(Map<Integer, Gallery> galleryMap) {
        if (galleryMap.isEmpty()) return;

        String sql = "SELECT gallery_id, exhibition_id, title, start_date, end_date, "
                + "description, curator_name, theme "
                + "FROM exhibition ORDER BY gallery_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int gid = rs.getInt("gallery_id");
                Gallery gallery = galleryMap.get(gid);
                if (gallery != null) {
                    Exhibition ex = new Exhibition();
                    ex.setTitle(rs.getString("title"));
                    ex.setStartDate(rs.getDate("start_date").toLocalDate());
                    ex.setEndDate(rs.getDate("end_date").toLocalDate());
                    ex.setDescription(rs.getString("description"));
                    ex.setCuratorName(rs.getString("curator_name"));
                    ex.setTheme(rs.getString("theme"));
                    ex.setGallery(gallery);
                    gallery.getExhibitions().add(ex);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading gallery exhibitions", e);
        }
    }
}
