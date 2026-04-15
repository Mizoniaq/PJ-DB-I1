package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artwork;
import java.util.List;

/**
 * JDBC implementation for ArtworkDao.
 */
public class JdbcArtworkDao implements ArtworkDao {

    @Override
    public List<Artwork> findAll() {
        throw new UnsupportedOperationException("JDBC Implementation not yet provided.");
    }

    @Override
    public void save(Artwork artwork) {
        throw new UnsupportedOperationException("JDBC Implementation not yet provided.");
    }

    @Override
    public void update(Artwork artwork) {
        throw new UnsupportedOperationException("JDBC Implementation not yet provided.");
    }

    @Override
    public void delete(String title) {
        throw new UnsupportedOperationException("JDBC Implementation not yet provided.");
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        throw new UnsupportedOperationException("JDBC Implementation not yet provided.");
    }
}
