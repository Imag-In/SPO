package org.icroco.picture.ui.persistence;

import lombok.RequiredArgsConstructor;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.mapper.CatalogMapper;
import org.icroco.picture.ui.model.mapper.MediaFileMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PersistenceService {
    private final CollectionRepository collectionRepo;
    private final MediaFileRepository mfRepo;
    private final CatalogMapper   colMapper;
    private final MediaFileMapper mfMapper;

    public Optional<Catalog> findCatalogById(int id) {
        return collectionRepo.findById(id)
                             .map(colMapper::map);
    }

    public List<Catalog> findAllCatalog() {
        return collectionRepo.findAll().stream().map(colMapper::map).toList();
    }

    public Catalog saveCatalog(@NonNull final Catalog catalog) {
        return colMapper.map(collectionRepo.saveAndFlush(colMapper.map(catalog)));
    }

    public void saveMediaFiles(Collection<MediaFile> files) {
        mfRepo.saveAll(files.stream().map(mfMapper::map).toList());
    }
}
