package org.icroco.picture.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.ImagInConfiguration;
import org.icroco.picture.event.CollectionUpdatedEvent;
import org.icroco.picture.event.CollectionsLoadedEvent;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.model.mapper.MediaCollectionMapper;
import org.icroco.picture.model.mapper.MediaFileMapper;
import org.icroco.picture.model.mapper.ThumbnailMapper;
import org.icroco.picture.persistence.model.MediaCollectionEntity;
import org.icroco.picture.views.task.TaskService;
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
    private final ThumbnailMapper       thMapper;
    @Qualifier(ImagInConfiguration.CATALOG)
    private final Cache                 mcCache;
    @Qualifier(ImagInConfiguration.THUMBNAILS)
    private final Cache                 thCache;
    private final TaskService           taskService;

    @EventListener(ApplicationStartedEvent.class)
    public void loadAllMediaCollection() {
        synchronized (collectionRepo) {
            Collection<MediaCollection> mediaCollections = collectionRepo.findAll()
                                                                         .stream()
                                                                         .map(colMapper::map)
                                                                         .peek(c -> mcCache.put(c.id(), c))
                                                                         .toList();
            taskService.sendEvent(CollectionsLoadedEvent.builder().mediaCollections(mediaCollections).source(this).build());
        }
    }

    @Transactional
//    @Cacheable(cacheNames = ImageInConfiguration.CATALOG, unless = "#result == null")
    public synchronized Optional<MediaCollection> findMediaCollection(int id) {
        return Optional.ofNullable(mcCache.get(id, MediaCollection.class));
    }

    /**
     * Identical to @{link {@link #findMediaCollection(int)}} but expect a non-empty result.
     */
    public MediaCollection getMediaCollection(int id) {
        return findMediaCollection(id).orElseThrow(() -> new IllegalStateException(
                "Technical Error: Collection not found: " + id));
    }

    @SuppressWarnings("unchecked")
    public Collection<MediaCollection> findAllMediaCollection() {
        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<Integer, MediaCollection>) mcCache.getNativeCache();
        return nativeCache.asMap().values();
//        return collectionRepo.findAll().stream().map(colMapper::map).toList();
    }

    public Optional<MediaCollection> findMediaCollectionForFile(Path path) {
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
            MediaCollectionEntity saved             = collectionRepo.saveAndFlush(colMapper.map(mediaCollection));
            var                   updatedCollection = colMapper.map(saved);

            mcCache.put(updatedCollection.id(), updatedCollection);
            log.info("Collection saved, id: '{}', path: '{}'", updatedCollection.id(), updatedCollection.path());
            return updatedCollection;
        }
    }

    @Transactional
    public void saveMediaFiles(Collection<MediaFile> files) {
        mfRepo.saveAll(files.stream().map(mfMapper::map).toList());
    }

    public void deleteMediaCollection(@NonNull MediaCollection mediaCollection) {
        deleteMediaCollection(mediaCollection.id());
    }

    @Transactional
//    @CacheEvict(cacheNames = ImageInConfiguration.CATALOG)
    public void deleteMediaCollection(int mediaCollectionId) {
        synchronized (collectionRepo) {
            log.info("MediaCollection delete, id: '{}'", mediaCollectionId);
            Optional<MediaCollection> catalogById = findMediaCollection(mediaCollectionId);
            log.info("thRepo size before: {}, thCache: {}",
                     thumbRepo.findAll().size(),
                     ((com.github.benmanes.caffeine.cache.Cache<?, ?>) thCache.getNativeCache()).asMap().size());
            collectionRepo.deleteById(mediaCollectionId);
            mcCache.evictIfPresent(mediaCollectionId);
            catalogById.ifPresent(mc -> {
//                thumbRepo.deleteAll();
                log.info("Thumbnail Id to be deleted (10 first): {}", mc.medias().stream().map(MediaFile::getId).limit(10).toList());
                thumbRepo.deleteAllById(mc.medias().stream().map(MediaFile::getId).toList());
                mc.medias().forEach(thCache::evict);
                thumbRepo.flush();
            });
            log.info("thRepo size after: {}, thCache: {}",
                     thumbRepo.findAll().size(),
                     ((com.github.benmanes.caffeine.cache.Cache<?, ?>) thCache.getNativeCache()).asMap().size());
        }
    }

    public Thumbnail saveOrUpdate(Thumbnail thumbnail) {
        synchronized (thumbRepo) {
            var dbThumbnail = thMapper.map(thumbnail);

            thumbRepo.save(dbThumbnail);

            return thumbnail;
        }
    }

    public List<Thumbnail> saveAll(Collection<Thumbnail> thumbnails) {
        return thumbRepo.saveAllAndFlush(thumbnails.stream()
                                                   .map(thMapper::map)
                                                   .toList())
                        .stream()
                        .map(thMapper::map)
                        .toList();
    }

    public List<MediaFile> saveAllMediaFiles(Collection<MediaFile> mediaFiles) {
        return mfRepo.saveAllAndFlush(mediaFiles.stream()
                                                .map(mfMapper::map)
                                                .toList())
                     .stream()
                     .map(mfMapper::map)
                     .toList();
    }

//    public Optional<MediaFile> findByPath(Path p) {
//        return mfRepo.findByFullPath(p).map(mfMapper::map);
//        // TODO: Load thumbnail ?
//    }

    public Optional<Thumbnail> getThumbnailFromCache(MediaFile mediaFile) {
        return Optional.ofNullable(thCache.get(mediaFile, Thumbnail.class));
    }

    public Optional<Thumbnail> findByPathOrId(MediaFile mediaFile) {
        return thumbRepo.findById(mediaFile.getId())
//        return thumbRepo.findByFullPath(mediaFile.fullPath())
//                        .orElseGet(() -> thumbRepo.findById(mediaFile.getId()))
                        .map(thMapper::map);
    }

    @Transactional
    public synchronized void updateCollection(int id,
                                              Collection<MediaFile> toBeAdded,
                                              Collection<MediaFile> toBeDeleted,
                                              boolean sendRefreshEvent) {
        var mc         = getMediaCollection(id);
        var mediaFiles = mc.medias();

        mediaFiles.removeIf(toBeDeleted::contains);
        var newRaws = toBeAdded.stream()
                               .map(mfMapper::map)
//                               .peek(dbMf -> dbMf.setMediaCollection(collectionRepo.getReferenceById(id)))
                               .toList();

        newRaws = mfRepo.saveAllAndFlush(newRaws);
        List<MediaFile> toBeAddedSaved = newRaws.stream().map(mfMapper::map).toList();
        mediaFiles.removeAll(toBeAdded);
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
        if (sendRefreshEvent) {
            taskService.sendEvent(CollectionUpdatedEvent.builder()
                                                        .mediaCollectionId(mc.id())
                                                        .newItems(toBeAddedSaved)
                                                        .deletedItems(toBeDeleted)
                                                        .source(this)
                                                        .build());
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
