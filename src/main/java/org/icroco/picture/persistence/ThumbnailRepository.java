package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.ThumbnailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThumbnailRepository extends JpaRepository<ThumbnailEntity, Long> {
    @Query(value = "SELECT * FROM thumbnail t WHERE t.FULL_PATH = :#{#path.toString()}", nativeQuery = true)
    Optional<ThumbnailEntity> findByFullPath(@Param("path") Path p);

    @Query(value = "SELECT t.mfId FROM ThumbnailEntity t WHERE t.mfId IN (:ids) ")
    List<Long> findAllMfIdByMfId(List<Long> ids);

    @Query(value = """
            SELECT  l.mfId
            FROM    ThumbnailEntity l
            LEFT JOIN
                    MediaFileEntity r
            ON      r.id = l.mfId
            WHERE   r.id IS NULL
            """)
    List<Long> findOrphelans();

    interface IdAndBytes {
        String getMfId();

        byte[] getImage();
    }

    Optional<IdAndBytes> findImageByMfId(Long mfId);

}
