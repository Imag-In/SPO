package org.icroco.picture.thumbnail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.ThumbnailRepository;
import org.icroco.picture.persistence.mapper.ThumbnailMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ThumbnailService {
    private final IThumbnailGenerator       generator;
    private final Map<MediaFile, Thumbnail> thCache;
    private final ThumbnailRepository       repository;
    private final ThumbnailMapper           mapper;

    public Optional<Thumbnail> get(MediaFile mf) {
        return get(mf, true);
    }

    public Optional<Thumbnail> get(MediaFile mf, boolean updateCache) {
        return Optional.ofNullable(thCache.get(mf))
                       .or(() -> repository.findById(mf.getId())
                                           .map(mapper::toDomain)
                                           .map(t -> {
                                               if (updateCache) {
                                                   thCache.put(mf, t);
                                               }
                                               return t;
                                           }));
    }

    public Thumbnail extract(MediaFile mf) {
        return saveAndUodateCache(mf, generator.extractThumbnail(mf.getFullPath()));
    }

    public Thumbnail generate(MediaFile mf) {
        return saveAndUodateCache(mf, generator.generate(mf.getFullPath()));
    }

//    public Either<Throwable, Image> generate(MediaFile mf, @NonNull Thumbnail thumbnail) {
//        var generated =  saveAndUodateCache(mf, generator.generate(mf.getFullPath()));
//
//        return generated.getOrigin() == EThumbnailType.GENERATED
//               ? Either.right(generated.getImage())
//               : Either.left(new ImagingException(generated.getLastErrorMessage()));
//    }

    private Thumbnail saveAndUodateCache(MediaFile mf, Thumbnail thumbnail) {
        thumbnail.setMfId(mf.getId());
        thumbnail = mapper.toDomain(repository.save(mapper.toEntity(thumbnail)));
        thCache.put(mf, thumbnail);

        return thumbnail;
    }
}
