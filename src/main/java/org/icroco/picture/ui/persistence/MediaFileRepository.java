package org.icroco.picture.ui.persistence;

import org.icroco.picture.ui.persistence.model.DbMediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<DbMediaFile, Long> {

    @Query(value = "SELECT * FROM media mf WHERE mf.FULL_PATH = :#{#path.toString()}", nativeQuery = true)
    Optional<DbMediaFile> findByFullPath(@Param("path") Path p);
}
