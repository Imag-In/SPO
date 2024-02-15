package org.icroco.picture.metadata;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.config.SpoConfiguration;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.persistence.KeywordRepository;
import org.icroco.picture.persistence.mapper.KeywordMapper;
import org.icroco.picture.persistence.model.KeywordEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeywordManager implements IKeywordManager {
    private final KeywordRepository    repository;
    private final KeywordMapper        mapper;
    @Qualifier(SpoConfiguration.CACHE_KEYWORD)
    private final Map<String, Keyword> cache;

    @PostConstruct
    void initCache() {
        Thread.startVirtualThread(() -> {
            repository.findAll()
                      .stream()
                      .map(mapper::toDomain)
                      .forEach(kw -> cache.put(kw.name(), kw));
        });
    }

    @Override
    @Cacheable(cacheNames = SpoConfiguration.CACHE_KEYWORD_RAW, unless = "#result == null")
    public Keyword findOrCreateTag(String name) {
        return mapper.toDomain(repository.findByName(name)
                                         .orElseGet(() -> repository.save(KeywordEntity.builder().name(name).build())));
    }

    @Override
    public Collection<String> getAll() {
        return cache.values().stream().map(Keyword::name).toList();
    }
}
