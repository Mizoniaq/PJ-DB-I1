package com.project.artconnect.service.impl;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import com.project.artconnect.service.CommunityService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed implementation of CommunityService.
 * Delegates to JdbcCommunityMemberDao which loads bookings and reviews.
 */
public class JdbcCommunityService implements CommunityService {

    private final CommunityMemberDao memberDao;

    public JdbcCommunityService(CommunityMemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public List<CommunityMember> getAllMembers() {
        return memberDao.findAll();
    }

    @Override
    public Optional<CommunityMember> getMemberByName(String name) {
        return memberDao.findAll().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Review> getReviewsByMember(CommunityMember member) {
        if (member == null) return Collections.emptyList();
        return member.getReviews();
    }
}
