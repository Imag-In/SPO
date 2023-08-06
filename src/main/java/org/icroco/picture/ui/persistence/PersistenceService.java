package org.icroco.picture.ui.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.config.ImageInConfiguration;
import org.icroco.picture.ui.event.CollectionUpdatedEvent;
import org.icroco.picture.ui.event.CollectionsLoadedEvent;
import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.model.mapper.MediaCollectionMapper;
import org.icroco.picture.ui.model.mapper.MediaFileMapper;
import org.icroco.picture.ui.model.mapper.ThumbnailMapper;
import org.icroco.picture.ui.persistence.model.DbMediaCollection;
import org.icroco.picture.ui.task.TaskService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cache.Cache;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unchecked")
@Component
@RequiredArgsConstructor
@Slf4j
public class PersistenceService {
    private final CollectionRepository  collectionRepo;
    private final MediaFileRepository   mfRepo;
    private final ThumbnailRepository   thumbRepo;
    private final MediaCollectionMapper colMapper;
    private final MediaFileMapper       mfMapper;
    private final ThumbnailMapper       thumbMapper;
    @Qualifier(ImageInConfiguration.CATALOG)
    private final Cache                 mcCache;
    private final TaskService           taskService;

    @EventListener(ApplicationStartedEvent.class)
    public void loadAllCatalog() {
        synchronized (collectionRepo) {
            Collection<MediaCollection> mediaCollections = collectionRepo.findAll()
                                                                         .stream()
                                                                         .map(colMapper::map)
                                                                         .peek(c -> mcCache.put(c.id(), c))
                                                                         .toList();
            taskService.sendFxEvent(new CollectionsLoadedEvent(mediaCollections, this));
        }
    }

    @Transactional
//    @Cacheable(cacheNames = ImageInConfiguration.CATALOG, unless = "#result == null")
    public synchronized Optional<MediaCollection> findCatalogById(int id) {
        return Optional.ofNullable(mcCache.get(id, MediaCollection.class));
    }

    /**
     * Identical to @{link {@link #findCatalogById(int)}} but expect a non-empty result.
     */
    public MediaCollection getCatalogById(int id) {
        return findCatalogById(id).orElseThrow(() -> new IllegalStateException(
                "Technical Error: Collection not found: " + id));
    }

    @SuppressWarnings("unchecked")
    public Collection<MediaCollection> findAllCatalog() {
        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<Integer, MediaCollection>) mcCache.getNativeCache();
        return nativeCache.asMap().values();
//        return collectionRepo.findAll().stream().map(colMapper::map).toList();
    }

    public Optional<MediaCollection> findCatalogForFile(Path path) {
        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<Integer, MediaCollection>) mcCache.getNativeCache();
        return nativeCache.asMap().values()
                          .stream()
                          .filter(mediaCollection -> path.startsWith(mediaCollection.path()))
                          .findFirst();
    }

    @Transactional
//    @CachePut(cacheNames = ImageInConfiguration.CATALOG, key = "#mediaCollection.id")
    public MediaCollection saveCollection(@NonNull final MediaCollection mediaCollection) {
        synchronized (collectionRepo) {
            DbMediaCollection saved             = collectionRepo.saveAndFlush(colMapper.map(mediaCollection));
            var               updatedCollection = colMapper.map(saved);

            mcCache.put(updatedCollection.id(), mediaCollection);
            log.info("Collection saved, id: '{}', path: '{}'", updatedCollection.id(), updatedCollection.path());
            return updatedCollection;
        }
    }

    @Transactional
    public void saveMediaFiles(Collection<MediaFile> files) {
        mfRepo.saveAll(files.stream().map(mfMapper::map).toList());
    }

    public void deleteCatalog(@NonNull MediaCollection mediaCollection) {
        deleteCatalog(mediaCollection.id());
    }

    @Transactional
//    @CacheEvict(cacheNames = ImageInConfiguration.CATALOG)
    public void deleteCatalog(int mediaCollectionId) {
        synchronized (collectionRepo) {
            log.info("MediaCollection deleted, id: '{}'", mediaCollectionId);
            collectionRepo.deleteById(mediaCollectionId);
            mcCache.evictIfPresent(mediaCollectionId);
        }
    }

    public Thumbnail saveOrUpdate(Thumbnail thumbnail) {
        var dbThumbnail = thumbMapper.map(thumbnail);

        thumbRepo.save(dbThumbnail);

        return thumbnail;
    }

    public List<Thumbnail> saveAll(Collection<Thumbnail> thumbnails) {
        return thumbRepo.saveAll(thumbnails.stream().map(thumbMapper::map).toList()).stream().map(thumbMapper::map).toList();
    }

//    public Optional<MediaFile> findByPath(Path p) {
//        return mfRepo.findByFullPath(p).map(mfMapper::map);
//        // TODO: Load thumbnail ?
//    }

    public Optional<Thumbnail> findByPathOrId(MediaFile mediaFile) {
        return thumbRepo.findByFullPath(mediaFile.fullPath())
//                        .orElseGet(() -> thumbRepo.findById(mediaFile.getId()))
                        .map(thumbMapper::map);
    }

    @Transactional
    public synchronized void updateCollection(int id, Collection<MediaFile> toBeAdded, Collection<MediaFile> toBeDeleted) {
        synchronized (collectionRepo) {
            var mc         = getCatalogById(id);
            var mediaFiles = mc.medias();

            mediaFiles.removeIf(toBeDeleted::contains);
            var newRaws = toBeAdded.stream()
                                   .map(mfMapper::map)
                                   .peek(dbMf -> dbMf.setMediaCollection(collectionRepo.getReferenceById(id)))
                                   .toList();

            newRaws = mfRepo.saveAll(newRaws);
            List<MediaFile> toBeAddedSaved = newRaws.stream().map(mfMapper::map).toList();
            mediaFiles.addAll(toBeAddedSaved);
            // just to delete values
            mc = saveCollection(mc);
            mcCache.put(mc.id(), mc);

//            var newUpdated = mc.medias().stream()
//                               .filter(toBeAdded::contains)
//                               .toList();
//            var delUpdated = mc.medias().stream()
//                               .filter(toBeDeleted::contains)
//                               .toList();
            taskService.sendFxEvent(new CollectionUpdatedEvent(mc.id(), toBeAddedSaved, toBeDeleted, this));
        }
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
