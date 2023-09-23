package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.DbTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<DbTag, Integer> {
}
