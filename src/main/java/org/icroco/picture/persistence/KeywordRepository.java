package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.KeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<KeywordEntity, Integer> {

    Optional<KeywordEntity> findByName(String name);
}
