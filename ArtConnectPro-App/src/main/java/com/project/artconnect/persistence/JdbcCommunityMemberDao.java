package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.*;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation for CommunityMemberDao.
 * Also loads bookings and reviews for each member.
 */
public class JdbcCommunityMemberDao implements CommunityMemberDao {

    @Override
    public Optional<CommunityMember> findById(Long id) {
        String sql = "SELECT member_id, name, email, birth_year, phone, city, membership_type "
                + "FROM community_member WHERE member_id = ?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int memberId = rs.getInt("member_id");
                    CommunityMember m = mapRow(rs);
                    loadBookings(conn, memberId, m);
                    loadReviews(conn, memberId, m);
                    return Optional.of(m);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching community member by id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<CommunityMember> findAll() {
        String sql = "SELECT member_id, name, email, birth_year, phone, city, membership_type "
                + "FROM community_member ORDER BY member_id";

        Map<Integer, CommunityMember> memberMap = new LinkedHashMap<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int memberId = rs.getInt("member_id");
                memberMap.put(memberId, mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all community members", e);
        }

        // Load bookings and reviews for all members
        loadBookingsForMembers(memberMap);
        loadReviewsForMembers(memberMap);

        return new ArrayList<>(memberMap.values());
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private CommunityMember mapRow(ResultSet rs) throws SQLException {
        CommunityMember m = new CommunityMember();
        m.setName(rs.getString("name"));
        m.setEmail(rs.getString("email"));
        m.setBirthYear(rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null);
        m.setPhone(rs.getString("phone"));
        m.setCity(rs.getString("city"));
        m.setMembershipType(rs.getString("membership_type"));
        return m;
    }

    // -- Bookings ---------------------------------------------------

    private void loadBookings(Connection conn, int memberId, CommunityMember member) throws SQLException {
        String sql = "SELECT b.booking_date, b.payment_status, "
                + "w.title AS workshop_title, w.date AS workshop_date, w.price AS workshop_price "
                + "FROM booking b "
                + "JOIN workshop w ON b.workshop_id = w.workshop_id "
                + "WHERE b.member_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    member.getBookings().add(mapBooking(rs, member));
                }
            }
        }
    }

    private void loadBookingsForMembers(Map<Integer, CommunityMember> memberMap) {
        if (memberMap.isEmpty()) return;

        String sql = "SELECT b.member_id, b.booking_date, b.payment_status, "
                + "w.title AS workshop_title, w.date AS workshop_date, w.price AS workshop_price "
                + "FROM booking b "
                + "JOIN workshop w ON b.workshop_id = w.workshop_id "
                + "ORDER BY b.member_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int memberId = rs.getInt("member_id");
                CommunityMember member = memberMap.get(memberId);
                if (member != null) {
                    member.getBookings().add(mapBooking(rs, member));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading member bookings", e);
        }
    }

    private Booking mapBooking(ResultSet rs, CommunityMember member) throws SQLException {
        Workshop w = new Workshop();
        w.setTitle(rs.getString("workshop_title"));
        Timestamp ts = rs.getTimestamp("workshop_date");
        if (ts != null) {
            w.setDate(ts.toLocalDateTime());
        }
        w.setPrice(rs.getDouble("workshop_price"));

        Booking b = new Booking();
        b.setWorkshop(w);
        b.setMember(member);
        Timestamp bookingTs = rs.getTimestamp("booking_date");
        if (bookingTs != null) {
            b.setBookingDate(bookingTs.toLocalDateTime());
        }
        b.setPaymentStatus(rs.getString("payment_status"));
        return b;
    }

    // -- Reviews ----------------------------------------------------

    private void loadReviews(Connection conn, int memberId, CommunityMember member) throws SQLException {
        String sql = "SELECT r.rating, r.comment, r.review_date, "
                + "aw.title AS artwork_title "
                + "FROM review r "
                + "JOIN artwork aw ON r.artwork_id = aw.artwork_id "
                + "WHERE r.reviewer_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    member.getReviews().add(mapReview(rs, member));
                }
            }
        }
    }

    private void loadReviewsForMembers(Map<Integer, CommunityMember> memberMap) {
        if (memberMap.isEmpty()) return;

        String sql = "SELECT r.reviewer_id, r.rating, r.comment, r.review_date, "
                + "aw.title AS artwork_title "
                + "FROM review r "
                + "JOIN artwork aw ON r.artwork_id = aw.artwork_id "
                + "ORDER BY r.reviewer_id";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int memberId = rs.getInt("reviewer_id");
                CommunityMember member = memberMap.get(memberId);
                if (member != null) {
                    member.getReviews().add(mapReview(rs, member));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading member reviews", e);
        }
    }

    private Review mapReview(ResultSet rs, CommunityMember member) throws SQLException {
        Artwork aw = new Artwork();
        aw.setTitle(rs.getString("artwork_title"));

        Review r = new Review();
        r.setReviewer(member);
        r.setArtwork(aw);
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        Date d = rs.getDate("review_date");
        if (d != null) {
            r.setReviewDate(d.toLocalDate());
        }
        return r;
    }
}
