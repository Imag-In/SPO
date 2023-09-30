package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.MediaFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFileEntity, Long> {

    @Query(value = "SELECT * FROM media mf WHERE mf.FULL_PATH = :#{#path.toString()}", nativeQuery = true)
    Optional<MediaFileEntity> findByFullPath(@Param("path") Path p);
}
