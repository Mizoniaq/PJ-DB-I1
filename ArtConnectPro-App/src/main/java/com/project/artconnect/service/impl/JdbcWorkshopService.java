package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed implementation of WorkshopService.
 * Delegates reads to JdbcWorkshopDao and handles bookings via direct JDBC.
 */
public class JdbcWorkshopService implements WorkshopService {

    private final WorkshopDao workshopDao;

    public JdbcWorkshopService(WorkshopDao workshopDao) {
        this.workshopDao = workshopDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        return workshopDao.findAll().stream()
                .filter(w -> w.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || member == null) return;

        String sql = "INSERT INTO booking (workshop_id, member_id, payment_status) "
                + "VALUES ("
                + "(SELECT workshop_id FROM workshop WHERE title = ? LIMIT 1), "
                + "(SELECT member_id FROM community_member WHERE name = ? LIMIT 1), "
                + "'PENDING')";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workshop.getTitle());
            ps.setString(2, member.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error booking workshop", e);
        }
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        if (member == null) return Collections.emptyList();

        String sql = "SELECT b.booking_date, b.payment_status, "
                + "w.title AS workshop_title, w.date AS workshop_date, w.price AS workshop_price "
                + "FROM booking b "
                + "JOIN workshop w ON b.workshop_id = w.workshop_id "
                + "JOIN community_member cm ON b.member_id = cm.member_id "
                + "WHERE cm.name = ?";

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Workshop w = new Workshop();
                    w.setTitle(rs.getString("workshop_title"));
                    Timestamp ts = rs.getTimestamp("workshop_date");
                    if (ts != null) w.setDate(ts.toLocalDateTime());
                    w.setPrice(rs.getDouble("workshop_price"));

                    Booking b = new Booking();
                    b.setWorkshop(w);
                    b.setMember(member);
                    Timestamp bookingTs = rs.getTimestamp("booking_date");
                    if (bookingTs != null) b.setBookingDate(bookingTs.toLocalDateTime());
                    b.setPaymentStatus(rs.getString("payment_status"));
                    bookings.add(b);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching bookings by member", e);
        }

        return bookings;
    }
}
