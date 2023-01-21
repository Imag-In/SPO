package org.icroco.picture.ui.persistence;

import org.icroco.picture.ui.persistence.model.DbThumbnail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public interface ThumbnailRepository extends JpaRepository<DbThumbnail, Long> {
    @Query(value = "SELECT * FROM thumbnail t WHERE t.FULL_PATH = :#{#path.toString()}", nativeQuery = true)
    Optional<DbThumbnail> findByFullPath(@Param("path") Path p);
}
