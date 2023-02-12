package org.icroco.picture.ui.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.model.Catalog;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.model.mapper.CatalogMapper;
import org.icroco.picture.ui.model.mapper.MediaFileMapper;
import org.icroco.picture.ui.model.mapper.ThumbnailMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersistenceService {
    private final CollectionRepository collectionRepo;
    private final MediaFileRepository  mfRepo;
    private final ThumbnailRepository  thumbRepo;
    private final CatalogMapper        colMapper;
    private final MediaFileMapper      mfMapper;
    private final ThumbnailMapper      thumbMapper;

    @Transactional
    public Optional<Catalog> findCatalogById(int id) {
        return collectionRepo.findById(id)
                             .map(colMapper::map);
    }

    @Transactional
    public List<Catalog> findAllCatalog() {
        return collectionRepo.findAll().stream().map(colMapper::map).toList();
    }

    @Transactional
    public Catalog saveCatalog(@NonNull final Catalog catalog) {
        return colMapper.map(collectionRepo.saveAndFlush(colMapper.map(catalog)));
    }

    @Transactional
    public void saveMediaFiles(Collection<MediaFile> files) {
        mfRepo.saveAll(files.stream().map(mfMapper::map).toList());
    }

    @Transactional
    public void deleteCatalog(@NonNull Catalog catalog) {
        log.info("Catalog deleted, id: '{}', path: '{}'", catalog.id(), catalog.path());
        collectionRepo.deleteById(catalog.id());
//        collectionRepo.findById(catalog.id()).ifPresent(collectionRepo::delete);
    }

    public Thumbnail saveOrUpdate(Thumbnail thumbnail) {
        var dbThumbnail = thumbMapper.map(thumbnail);

        thumbRepo.save(dbThumbnail);

        return thumbnail;
    }

    public void saveAll(Collection<Thumbnail> thumbnails) {
        thumbRepo.saveAll(thumbnails.stream().map(thumbMapper::map).toList());
    }

    public Optional<MediaFile> findByPath(Path p) {
        return mfRepo.findByFullPath(p).map(mfMapper::map);
        // TODO: Load thumbnail ?
    }

    public Optional<Thumbnail> findByPathOrId(MediaFile mediaFile) {
        return thumbRepo.findByFullPath(mediaFile.fullPath())
//                        .orElseGet(() -> thumbRepo.findById(mediaFile.getId()))
                        .map(thumbMapper::map);
    }

//    public List<Thumbnail> saveAll(List<Thumbnail> values) {
//        return thumbRepo.saveAll(values.stream()
//                                       .map(thumbMapper::map)
//                                       .toList())
//                        .stream()
//                        .map(thumbMapper::map)
//                        .toList();
//    }
}
