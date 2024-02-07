package org.icroco.picture.persistence;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.SpoConfiguration;
import org.icroco.picture.event.CollectionsLoadedEvent;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.model.*;
import org.icroco.picture.persistence.mapper.MediaCollectionEntryMapper;
import org.icroco.picture.persistence.mapper.MediaCollectionMapper;
import org.icroco.picture.persistence.mapper.MediaFileMapper;
import org.icroco.picture.persistence.mapper.ThumbnailMapper;
import org.icroco.picture.persistence.model.MediaCollectionEntryEntity;
import org.icroco.picture.persistence.model.MediaFileEntity;
import org.icroco.picture.persistence.model.MfDuplicate;
import org.icroco.picture.views.task.TaskService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cache.Cache;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unchecked")
@Component
@RequiredArgsConstructor
@Slf4j
public class PersistenceService {
    private final CollectionRepository           collectionRepo;
    private final MediaFileRepository            mfRepo;
    private final ThumbnailRepository            thumbRepo;
    private final MediaCollectionEntryRepository mceRepo;
    private final IHashGenerator                 hashGenerator;
    private final MediaCollectionMapper          colMapper;
    private final MediaFileMapper                mfMapper;
    private final ThumbnailMapper                thMapper;
    private final MediaCollectionEntryMapper     mceMapper;
    @Qualifier(SpoConfiguration.CACHE_CATALOG)
    private final Cache                          mcCache;
    @Qualifier(SpoConfiguration.CACHE_THUMBNAILS_RAW)
    private final Map<MediaFile, Thumbnail>      thCache;
    private final TaskService                    taskService;
    private final ReentrantReadWriteLock         mcLock = new ReentrantReadWriteLock();

    @EventListener(ApplicationStartedEvent.class)
    public void loadAllMediaCollection() {
        try {
            mcLock.readLock().lock();
            Collection<MediaCollection> mediaCollections = collectionRepo.findAll()
                                                                         .stream()
                                                                         .map(colMapper::toDomain)
                                                                         .peek(c -> mcCache.put(c.id(), c))
                                                                         .toList();
            taskService.sendEvent(CollectionsLoadedEvent.builder().mediaCollections(mediaCollections).source(this).build());
        } finally {
            mcLock.readLock().unlock();
        }
//        Thread.ofVirtual().name("DataBase checking").start(Unchecked.runnable(() -> {
//            Thread.sleep(ofMinutes(1)); // It cost nothing with Virtual Thread. // Put in conf.
//            cleanOrphans();
//        }));
    }

    @Transactional
    public void cleanOrphans() {
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
                STR."Technical Error: Collection not found: \{id}"));
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
//    @CachePut(cacheNames = ImageInConfiguration.CATALOG, key = "#mediaCollection.mcId")
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
            log.info("Collection entitySaved, mcId: '{}', path: '{}'", updatedCollection.id(), updatedCollection.path());
            return updatedCollection;
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
//    @CachePut(cacheNames = ImageInConfiguration.CATALOG, key = "#mediaCollection.mcId")
    public MediaFile saveMediaFile(int mediaCollectionId, MediaFile mf) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();

            log.info("Save mediaFile: {}, collectionId: {}", mf, mediaCollectionId);

            var entity = mfMapper.toEntity(mf);
            entity.setCollectionId(mediaCollectionId);
            var entitySaved = mfRepo.saveAndFlush(entity);
            var updatedMf   = mfMapper.toDomainFromEntity(entitySaved, mf);

            findMediaCollection(mediaCollectionId).ifPresent(mcCache -> {
                mcCache.medias().remove(updatedMf);
                mcCache.medias().add(updatedMf);
            });
            log.info("mediaFile saved, mcId: '{}', path: '{}'", updatedMf.id(), updatedMf.fullPath());
            return updatedMf;
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
    public void saveMediaFile(MediaFile file, Consumer<MediaFile> preAction) {
        if (preAction != null) {
            preAction.accept(file);
            hashGenerator.compute(file.fullPath()).ifPresent(h -> {
                file.setHash(h);
                file.setHashDate(LocalDate.now());
                log.debug("Hash: '{}'", file.getHash());
            });
        }
        mfMapper.fromEntityToDomain(mfRepo.save(mfMapper.toEntity(file)), file);
    }

    @Transactional
    public void saveMediaFile(MediaFile file) {
        saveMediaFile(file, null);
    }

    @Transactional
    public void deleteMediaCollection(int mediaCollectionId) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            log.info("MediaCollection delete, mcId: '{}'", mediaCollectionId);
            collectionRepo.deleteById(mediaCollectionId);
            collectionRepo.flush();
            findMediaCollection(mediaCollectionId).ifPresent(mc -> {
                thumbRepo.deleteAllById(mc.medias().stream().map(MediaFile::getId).toList());
                thumbRepo.flush();
                mc.medias().forEach(thCache::remove);
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

    @Transactional
    public Thumbnail save(Thumbnail thumbnail) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
            thumbRepo.deleteById(thumbnail.getMfId());
            return thMapper.toDomain(thumbRepo.saveAndFlush(thMapper.toEntity(thumbnail)));
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
    public Thumbnail save(MediaFile mf, Thumbnail thumbnail) {
        var wLock = mcLock.writeLock();
        try {
            wLock.lock();
//            thumbnail.setDimension(new Dimension(thumbnail.getImage().getWidth(), thumbnail.getImage().getHeight()));
            mf.setThumbnailType(thumbnail.getOrigin());
            saveMediaFile(mf);
            return save(thumbnail);
        } finally {
            wLock.unlock();
        }
    }


    public Optional<Thumbnail> getThumbnailFromCache(MediaFile mediaFile) {
        return Optional.ofNullable(thCache.get(mediaFile));
    }

    public Optional<Thumbnail> findByPathOrId(MediaFile mediaFile) {
        return thumbRepo.findById(mediaFile.getId())
                        .map(thMapper::toDomain);
    }

    public List<HashDuplicate> findDuplicateByHash() {
        StopWatch stopWatch = new StopWatch("findAllDuplicate");
        stopWatch.start("Db");
        var dupByHash = mfRepo.findAllDuplicate()
                              .stream()
                              .collect(Collectors.groupingBy(MfDuplicate::getHash,
                                                             HashMap::new,
                                                             Collectors.mapping(identity(), toList())))
                              .entrySet();
        stopWatch.stop();
        stopWatch.start("Map");

        var dup = dupByHash.stream()
                           .map(e -> new HashDuplicate(e.getKey(),
                                                       mfMapper.toDomains(mfRepo.findAllById(e.getValue()
                                                                                              .stream()
                                                                                              .map(MfDuplicate::getId)
                                                                                              .toList()))))
                           .toList();

        stopWatch.stop();
        log.info("findAllDuplicate: {}", stopWatch.prettyPrint(TimeUnit.MILLISECONDS));

        return dup;
    }

    public record UpdateResult(Collection<MediaFile> added,
                               Collection<MediaFile> deleted,
                               Collection<MediaFile> updated) {
    }

    @Transactional
    public UpdateResult updateCollection(int id,
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
                mfRepo.deleteAllById(toBeDeleted.stream()
                                                .map(mfMapper::toEntity)
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

                //Update sub-path
                Set<Path> newParent = toBeAddedEntities.stream()
                                                       .map(MediaFileEntity::getFullPath)
                                                       .map(Path::getParent)
                                                       .collect(Collectors.toSet());

                var subDirByPath = mc.subPaths().stream().collect(Collectors.toMap(MediaCollectionEntry::name, Function.identity()));
                newParent = newParent.stream().filter(p -> !subDirByPath.containsKey(p)).collect(Collectors.toSet());
                var newEntries = mceRepo.saveAll(newParent.stream().map(p -> new MediaCollectionEntryEntity(null, p, mc.id())).toList());
                mc.getSubPaths().addAll(newEntries.stream().map(mceMapper::toDomain).toList());
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
                                                                               .map(mfEntity -> mfMapper.fromEntityToDomain(mfEntity,
                                                                                                                            mediaFile))
                                                                               .orElse(null))
                                                       .filter(Objects::nonNull)
                                                       .toList();
                mfRepo.flush();
                // TODO: ? Should be optional since we reuse MediaFile object.
//                updatedMediaFiles.forEach(mediaFile -> {
//                    mediaFiles.remove(mediaFile);
//                    mediaFiles.add(mediaFile);
//                });
                toBeModifiedSaved = updatedMediaFiles;
            }

            return new UpdateResult(toBeAddedSaved, toBeDeleted, toBeModifiedSaved);
        } finally {
            wLock.unlock();
        }
    }

    @Transactional
    public Set<Long> removeSubDirFromCollection(int id, Set<Path> subdirs) {
        if (subdirs.isEmpty()) {
            return null;
        }
        try {
            mcLock.writeLock().lock();
            final var mc = getMediaCollection(id);

            var ids = mc.subPaths()
                        .stream()
                        .filter(mce -> subdirs.contains(mce.name()))
                        .map(MediaCollectionEntry::id)
                        .collect(Collectors.toSet());

            mc.subPaths().removeIf(mce -> ids.contains(mce.id()));
            mceRepo.deleteAllById(ids);
            return ids;
        } finally {
            mcLock.writeLock().unlock();
        }
    }


    public long countMediaFiles() {
        return mfRepo.count();
    }

    public List<CollectionRepository.IdAndPath> findAllIdAndPath() {
        return collectionRepo.findAllProjectedBy();
    }

}
