package com.project.artconnect.util;

import com.project.artconnect.persistence.*;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

/**
 * Service Provider to manage singleton instances of services.
 *
 * Uses JDBC-backed services connected to the ArtConnect MySQL database.
 * The InMemoryXxxService implementations are kept in the codebase for
 * testing purposes but are no longer used by the application.
 */
public class ServiceProvider {

    // --- DAOs ---
    private static final JdbcArtistDao artistDao = new JdbcArtistDao();
    private static final JdbcArtworkDao artworkDao = new JdbcArtworkDao();
    private static final JdbcExhibitionDao exhibitionDao = new JdbcExhibitionDao();
    private static final JdbcGalleryDao galleryDao = new JdbcGalleryDao();
    private static final JdbcWorkshopDao workshopDao = new JdbcWorkshopDao();
    private static final JdbcCommunityMemberDao communityMemberDao = new JdbcCommunityMemberDao();

    // --- Services (JDBC-backed) ---
    private static final ArtistService artistService = new JdbcArtistService(artistDao);
    private static final ArtworkService artworkService = new JdbcArtworkService(artworkDao);
    private static final GalleryService galleryService = new JdbcGalleryService(galleryDao);
    private static final WorkshopService workshopService = new JdbcWorkshopService(workshopDao);
    private static final CommunityService communityService = new JdbcCommunityService(communityMemberDao);

    public static ArtistService getArtistService() {
        return artistService;
    }

    public static ArtworkService getArtworkService() {
        return artworkService;
    }

    public static GalleryService getGalleryService() {
        return galleryService;
    }

    public static WorkshopService getWorkshopService() {
        return workshopService;
    }

    public static CommunityService getCommunityService() {
        return communityService;
    }
}
