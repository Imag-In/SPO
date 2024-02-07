package org.icroco.picture.persistence;

import org.icroco.picture.persistence.model.MediaCollectionEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaCollectionEntryRepository extends JpaRepository<MediaCollectionEntryEntity, Long> {

}
