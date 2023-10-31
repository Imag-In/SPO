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
import org.icroco.picture.persistence.mapper.MediaCollectionMapper;
import org.icroco.picture.persistence.mapper.MediaFileMapper;
import org.icroco.picture.persistence.mapper.ThumbnailMapper;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("unchecked")
@Component
@RequiredArgsConstructor
@Slf4j
public class PersistenceService {
    private final CollectionRepository   collectionRepo;
    private final MediaFileRepository    mfRepo;
    private final ThumbnailRepository    thumbRepo;
    private final MediaCollectionMapper  colMapper;
    private final MediaFileMapper        mfMapper;
    private final ThumbnailMapper        thMapper;
    @Qualifier(ImagInConfiguration.CACHE_CATALOG)
    private final Cache                  mcCache;
    @Qualifier(ImagInConfiguration.CACHE_THUMBNAILS)
    private final Cache                  thCache;
    private final TaskService            taskService;
    private final ReentrantReadWriteLock mcLock = new ReentrantReadWriteLock();

    @EventListener(ApplicationStartedEvent.class)
    public void loadAllMediaCollection() {
        var rLock = mcLock.readLock();
        try {
            rLock.lock();
            Collection<MediaCollection> mediaCollections = collectionRepo.findAll()
                                                                         .stream()
                                                                         .map(colMapper::toDomain)
                                                                         .peek(c -> mcCache.put(c.id(), c))
                                                                         .toList();
            taskService.sendEvent(CollectionsLoadedEvent.builder().mediaCollections(mediaCollections).source(this).build());
        } finally {
            rLock.unlock();
        }
    }

    @Transactional
//    @Cacheable(cacheNames = ImageInConfiguration.CATALOG, unless = "#result == null")
    public Optional<MediaCollection> findMediaCollection(int id) {
        return Optional.ofNullable(mcCache.get(id, MediaCollection.class));
    }

    /**
     * Identical to @{link {@link #findMediaCollection(int)}} but expect a non-empty result.
     */
    @Transactional
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
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();

            log.info("Save collection: {}", mediaCollection);
            var mcEntity = colMapper.toEntity(mediaCollection);

            var entitySaved = collectionRepo.saveAndFlush(mcEntity);
            entitySaved.getMedias()
                       .forEach(mf -> mf.setCollectionId(entitySaved.getId())); // TODO: don't understant why I have to do this !
            var updatedCollection = colMapper.toDomain(entitySaved);

            mcCache.put(updatedCollection.id(), updatedCollection);
            log.info("Collection entitySaved, id: '{}', path: '{}'", updatedCollection.id(), updatedCollection.path());
            return updatedCollection;
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
//    @CachePut(cacheNames = ImageInConfiguration.CATALOG, key = "#mediaCollection.id")
    public MediaFile saveMediaFile(int mediaCollectionId, MediaFile mf) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();

            log.info("Save mediaFile: {}, collectionId: {}", mf, mediaCollectionId);

            var entity = mfMapper.toEntity(mf);
            entity.setCollectionId(mediaCollectionId);
            var entitySaved = mfRepo.saveAndFlush(entity);
            var updatedMf   = mfMapper.toDomain(entitySaved);

            findMediaCollection(mediaCollectionId).ifPresent(mcCache -> {
                mcCache.medias().remove(updatedMf);
                mcCache.medias().add(updatedMf);
            });
            log.info("mediaFile saved, id: '{}', path: '{}'", updatedMf.id(), updatedMf.fullPath());
            return updatedMf;
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
    public void saveMediaFiles(Collection<MediaFile> files) {
        mfRepo.saveAll(files.stream().map(mfMapper::toEntity).toList());
    }

    @Transactional
//    @CacheEvict(cacheNames = ImageInConfiguration.CATALOG)
    public void deleteMediaCollection(int mediaCollectionId) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            log.info("MediaCollection delete, id: '{}'", mediaCollectionId);
            collectionRepo.deleteById(mediaCollectionId);
            collectionRepo.flush();
            findMediaCollection(mediaCollectionId).ifPresent(mc -> {
                thumbRepo.deleteAllById(mc.medias().stream().map(MediaFile::getId).toList());
                thumbRepo.flush();
                mc.medias().forEach(thCache::evict);
            });
            mcCache.evictIfPresent(mediaCollectionId);
        } finally {
            wLock.unlock();
        }
    }

    public List<Thumbnail> saveAll(Collection<Thumbnail> thumbnails) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            return thumbRepo.saveAllAndFlush(thumbnails.stream()
                                                       .map(thMapper::toEntity)
                                                       .toList())
                            .stream()
                            .map(thMapper::toDomain)
                            .toList();
        } finally {
            wLock.unlock();
        }
    }


    public Optional<Thumbnail> getThumbnailFromCache(MediaFile mediaFile) {
        return Optional.ofNullable(thCache.get(mediaFile, Thumbnail.class));
    }

    public Optional<Thumbnail> findByPathOrId(MediaFile mediaFile) {
        return thumbRepo.findById(mediaFile.getId())
//        return thumbRepo.findByFullPath(mediaFile.fullPath())
//                        .orElseGet(() -> thumbRepo.findById(mediaFile.getId()))
                        .map(thMapper::toDomain);
    }

//    @Transactional
//    public void updateCollection(int id,
//                                              Collection<MediaFile> toBeAdded,
//                                              Collection<MediaFile> toBeDeleted,
//                                              boolean sendRefreshEvent) {
//        var wLock = mcLock.writeLock();
//        try {
//            wLock.lock();
//            var mc         = getMediaCollection(id);
//            var mediaFiles = mc.medias();
//
//            mediaFiles.removeIf(toBeDeleted::contains);
//            var newRaws = toBeAdded.stream()
//                                   .map(mfMapper::map)
////                               .peek(dbMf -> dbMf.setMediaCollection(collectionRepo.getReferenceById(id)))
//                                   .toList();
//
//            newRaws = mfRepo.saveAllAndFlush(newRaws);
//            List<MediaFile> toBeAddedSaved = newRaws.stream().map(mfMapper::map).toList();
//            mediaFiles.removeAll(toBeAdded);
//            mediaFiles.addAll(toBeAddedSaved);
//            // just to delete values
//            mc = saveCollection(mc);
//            mcCache.put(mc.id(), mc);
//
////            var newUpdated = mc.medias().stream()
////                               .filter(toBeAdded::contains)
////                               .toList();
////            var delUpdated = mc.medias().stream()
////                               .filter(toBeDeleted::contains)
////                               .toList();
//            if (sendRefreshEvent) {
//                taskService.sendEvent(CollectionUpdatedEvent.builder()
//                                                            .mediaCollectionId(mc.id())
//                                                            .newItems(toBeAddedSaved)
//                                                            .deletedItems(toBeDeleted)
//                                                            .source(this)
//                                                            .build());
//            }
//        } finally {
//            wLock.unlock();
//        }
//    }

    @Transactional
    public void updateCollection(int id,
                                 Collection<MediaFile> toBeAdded,
                                 Collection<MediaFile> toBeDeleted,
                                 boolean sendRefreshEvent) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            final var mc         = getMediaCollection(id);
            final var mediaFiles = mc.medias();

            mfRepo.deleteAllById(toBeDeleted.stream().map(MediaFile::getId).toList());
            mediaFiles.removeIf(toBeDeleted::contains);

            var mfEntities = toBeAdded.stream()
                                      .map(mfMapper::toEntity)
                                      .peek(dbMf -> dbMf.setCollectionId(mc.id()))
                                      .toList();

            mfEntities = mfRepo.saveAllAndFlush(mfEntities);
            mediaFiles.removeAll(toBeAdded);

            var toBeAddedSaved = mfEntities.stream().map(mfMapper::toDomain).toList();
            mediaFiles.addAll(toBeAddedSaved);

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
        } finally {
            wLock.unlock();
        }
    }
}
