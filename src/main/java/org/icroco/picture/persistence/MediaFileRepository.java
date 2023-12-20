package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.MediaFileEntity;
import org.icroco.picture.persistence.model.MfDuplicate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFileEntity, Long> {

    @Query(value = "SELECT * FROM media mf WHERE mf.FULL_PATH = :#{#path.toString()}", nativeQuery = true)
    Optional<MediaFileEntity> findByFullPath(@Param("path") Path p);


    @Query(nativeQuery = true, value = """
            SELECT m1.ID, m1.HASH, m1.FULL_PATH FROM MEDIA m1
            JOIN (SELECT HASH, ORIGINAL_DATE, COUNT(*) FROM MEDIA GROUP BY HASH HAVING COUNT(*) > 1) m2
            ON m1.HASH = m2.HASH
                AND m1.ORIGINAL_DATE = m2.ORIGINAL_DATE
                """)
    List<MfDuplicate> findAllDuplicate();
}
