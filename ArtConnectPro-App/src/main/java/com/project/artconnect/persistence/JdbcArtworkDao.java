package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation for ArtworkDao.
 * Reconstructs Artist references when loading artworks.
 */
public class JdbcArtworkDao implements ArtworkDao {

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------

    @Override
    public List<Artwork> findAll() {
        String sql = "SELECT aw.artwork_id, aw.title, aw.creation_year, aw.type, aw.medium, "
                + "aw.dimensions, aw.description, aw.price, aw.status, "
                + "a.artist_id, a.name AS artist_name, a.bio, a.birth_year, "
                + "a.contact_email, a.city "
                + "FROM artwork aw "
                + "JOIN artist a ON aw.artist_id = a.artist_id "
                + "ORDER BY aw.artwork_id";

        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                artworks.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all artworks", e);
        }

        return artworks;
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        String sql = "SELECT aw.artwork_id, aw.title, aw.creation_year, aw.type, aw.medium, "
                + "aw.dimensions, aw.description, aw.price, aw.status, "
                + "a.artist_id, a.name AS artist_name, a.bio, a.birth_year, "
                + "a.contact_email, a.city "
                + "FROM artwork aw "
                + "JOIN artist a ON aw.artist_id = a.artist_id "
                + "WHERE a.name = ? "
                + "ORDER BY aw.artwork_id";

        List<Artwork> artworks = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, artistName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    artworks.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching artworks by artist name", e);
        }

        return artworks;
    }

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------

    @Override
    public void save(Artwork artwork) {
        String sql = "INSERT INTO artwork (title, creation_year, type, medium, dimensions, "
                + "description, price, status, artist_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, "
                + "(SELECT artist_id FROM artist WHERE name = ? LIMIT 1))";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, artwork.getTitle());
            ps.setObject(2, artwork.getCreationYear(), Types.INTEGER);
            ps.setString(3, artwork.getType());
            ps.setString(4, artwork.getMedium());
            ps.setString(5, artwork.getDimensions());
            ps.setString(6, artwork.getDescription());
            ps.setDouble(7, artwork.getPrice());
            ps.setString(8, artwork.getStatus() != null ? artwork.getStatus().name() : "FOR_SALE");
            ps.setString(9, artwork.getArtist() != null ? artwork.getArtist().getName() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving artwork", e);
        }
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------

    @Override
    public void update(Artwork artwork) {
        String sql = "UPDATE artwork SET creation_year = ?, type = ?, medium = ?, "
                + "dimensions = ?, description = ?, price = ?, status = ?, "
                + "artist_id = (SELECT artist_id FROM artist WHERE name = ? LIMIT 1) "
                + "WHERE title = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, artwork.getCreationYear(), Types.INTEGER);
            ps.setString(2, artwork.getType());
            ps.setString(3, artwork.getMedium());
            ps.setString(4, artwork.getDimensions());
            ps.setString(5, artwork.getDescription());
            ps.setDouble(6, artwork.getPrice());
            ps.setString(7, artwork.getStatus() != null ? artwork.getStatus().name() : "FOR_SALE");
            ps.setString(8, artwork.getArtist() != null ? artwork.getArtist().getName() : null);
            ps.setString(9, artwork.getTitle());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating artwork", e);
        }
    }

    // ---------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------

    @Override
    public void delete(String title) {
        String sql = "DELETE FROM artwork WHERE title = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting artwork", e);
        }
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    /**
     * Maps a ResultSet row to an Artwork with its Artist reference.
     */
    private Artwork mapRow(ResultSet rs) throws SQLException {
        Artist artist = new Artist();
        artist.setName(rs.getString("artist_name"));
        artist.setBio(rs.getString("bio"));
        artist.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
        artist.setContactEmail(rs.getString("contact_email"));
        artist.setCity(rs.getString("city"));

        Artwork aw = new Artwork();
        aw.setTitle(rs.getString("title"));
        aw.setCreationYear(rs.getObject("creation_year") != null ? rs.getInt("creation_year") : null);
        aw.setType(rs.getString("type"));
        aw.setMedium(rs.getString("medium"));
        aw.setDimensions(rs.getString("dimensions"));
        aw.setDescription(rs.getString("description"));
        aw.setPrice(rs.getDouble("price"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            aw.setStatus(Artwork.Status.valueOf(statusStr));
        }
        aw.setArtist(artist);
        return aw;
    }
}
