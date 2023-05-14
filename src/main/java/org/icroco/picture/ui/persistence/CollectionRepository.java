package org.icroco.picture.ui.persistence;

import org.icroco.picture.ui.persistence.model.DbMediaCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Perf: https://www.fusion-reactor.com/blog/finding-and-fixing-spring-data-jpa-performance-issues-with-fusionreactor/
@Repository
public interface CollectionRepository extends JpaRepository<DbMediaCollection, Integer> {
    //    @Query("SELECT c FROM catalogs c LEFT JOIN FETCH c.sub_path_id")
    @Override
    List<DbMediaCollection> findAll();
}
