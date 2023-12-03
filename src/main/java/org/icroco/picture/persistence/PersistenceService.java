package org.icroco.picture.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.ImagInConfiguration;
import org.icroco.picture.event.CollectionsLoadedEvent;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.mapper.MediaCollectionMapper;
import org.icroco.picture.persistence.mapper.MediaFileMapper;
import org.icroco.picture.persistence.mapper.ThumbnailMapper;
import org.icroco.picture.persistence.model.MediaFileEntity;
import org.icroco.picture.views.task.TaskService;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cache.Cache;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.time.Duration.ofMinutes;

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
        Thread.ofVirtual().name("DataBase checking").start(Unchecked.runnable(() -> {
            Thread.sleep(ofMinutes(1)); // It cost nothing with Virtual Thread. // Put in conf.
            checkDatase();
        }));
    }

    private void checkDatase() {
        deleteOrphanThumbnails();
        cleanOprhanKeywords();
    }

    private void cleanOprhanKeywords() {
        // TODO: Clean MF_KEYWORDS association table
        // TODO: Clean Keyword table ? mught be useful for futur ?
    }

    private void deleteOrphanThumbnails() {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            // Make sure only one thread is cleaning
            log.debug("Database cleaning - starting");
            List<Long> orphelans = thumbRepo.findOrphelans();
            log.warn("Found '{}' thumbnail(s) orphelan(s), 20th first: {}",
                     orphelans.size(),
                     orphelans.stream().limit(20).map(Object::toString).collect(Collectors.joining(",")));
            thumbRepo.deleteAllById(orphelans);
            log.debug("Database cleaning - end.");
        } finally {
            wLock.unlock();
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
            deleteOrphanThumbnails();
            // TODO: Send notification.
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
    public List<Thumbnail> saveAll(Collection<Thumbnail> thumbnails) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            thumbRepo.deleteAllById(thumbnails.stream().map(Thumbnail::getMfId).toList());
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
                        .map(thMapper::toDomain);
    }

    public record UpdareResult(Collection<MediaFile> added,
                               Collection<MediaFile> deleted,
                               Collection<MediaFile> updated) {
    }

    @Transactional
    public UpdareResult updateCollection(int id,
                                         Set<MediaFile> toBeAdded,
                                         Set<MediaFile> toBeDeleted,
                                         Set<MediaFile> hasBeenModified) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            final var mc         = getMediaCollection(id);
            final var mediaFiles = mc.medias();

            // Proces items to DELETE
            if (!toBeDeleted.isEmpty()) {
                mfRepo.deleteAllById(toBeDeleted.stream().map(mfMapper::toEntity)
                                                .map(mce -> mfRepo.findByFullPath(mce.getFullPath()).orElse(null))
                                                .filter(Objects::nonNull)
                                                .map(MediaFileEntity::getId)
                                                .toList());
                mediaFiles.removeIf(toBeDeleted::contains);
            }

            // Proces items to ADD
            List<MediaFile> toBeAddedSaved = Collections.emptyList();
            if (!toBeAdded.isEmpty()) {
                var toBeAddedEntities = toBeAdded.stream()
                                                 .map(mfMapper::toEntity)
                                                 .peek(dbMf -> dbMf.setCollectionId(mc.id()))
                                                 .toList();
                toBeAddedEntities = mfRepo.saveAllAndFlush(toBeAddedEntities);
                mediaFiles.removeAll(toBeAdded);
                toBeAddedSaved = toBeAddedEntities.stream().map(mfMapper::toDomain).toList();
                mediaFiles.addAll(toBeAddedSaved);
            }

            // Proces items to UPDATE
            List<MediaFile> toBeModifiedSaved = Collections.emptyList();
            if (!hasBeenModified.isEmpty()) {
                var updatedMediaFiles = hasBeenModified.stream()
                                                       .map(mediaFile -> mfRepo.findById(mediaFile.getId())
                                                                               .map(entity -> {
                                                                                   mfMapper.toEntityFromDomain(mediaFile, entity);
                                                                                   entity.setCollectionId(mc.id());
                                                                                   return mfRepo.save(entity);
                                                                               })
                                                                               .map(mfMapper::toDomain)
                                                                               .orElse(null))
                                                       .filter(Objects::nonNull)
                                                       .toList();
                mfRepo.flush();
                updatedMediaFiles.forEach(mediaFile -> {
                    mediaFiles.remove(mediaFile);
                    mediaFiles.add(mediaFile);
                });
                toBeModifiedSaved = updatedMediaFiles;
            }

            return new UpdareResult(toBeAddedSaved, toBeDeleted, toBeModifiedSaved);
        } finally {
            wLock.unlock();
        }
    }

    public long countMediaFiles() {
        return mfRepo.count();
    }
}
