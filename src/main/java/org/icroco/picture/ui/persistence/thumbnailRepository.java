package org.icroco.picture.ui.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface thumbnailRepository extends JpaRepository<DbThumbnail, Integer> {
}
