package org.icroco.picture.metadata;

import lombok.RequiredArgsConstructor;
import org.icroco.picture.config.ImagInConfiguration;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.persistence.KeywordRepository;
import org.icroco.picture.persistence.mapper.KeywordMapper;
import org.icroco.picture.persistence.model.KeywordEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KeywordManager implements IKeywordManager {
    private final KeywordRepository repository;
    private final KeywordMapper     mapper;

    @Override
    @Cacheable(cacheNames = ImagInConfiguration.CACHE_KEYWORD, unless = "#result == null")
    public Keyword findOrCreateTag(String name) {
        return mapper.toDomain(repository.findByName(name)
                                         .orElseGet(() -> repository.save(KeywordEntity.builder().name(name).build())));
    }

}
