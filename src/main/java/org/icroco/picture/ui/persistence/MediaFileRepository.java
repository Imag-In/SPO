package org.icroco.picture.ui.persistence;

import org.icroco.picture.ui.persistence.model.DbMediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<DbMediaFile, Integer> {
}
