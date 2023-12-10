package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.MediaCollectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

// Perf: https://www.fusion-reactor.com/blog/finding-and-fixing-spring-data-jpa-performance-issues-with-fusionreactor/
@Repository
public interface CollectionRepository extends JpaRepository<MediaCollectionEntity, Integer> {
    //    @Query("SELECT c FROM catalogs c LEFT JOIN FETCH c.sub_path_id")
    @NonNull
    @Override
    List<MediaCollectionEntity> findAll();

    interface IdAndPath {
        int getId();

        Path getPath();
    }

    List<IdAndPath> findAllProjectedBy();

}
