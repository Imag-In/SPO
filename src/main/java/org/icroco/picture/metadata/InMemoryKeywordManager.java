package org.icroco.picture.metadata;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.config.SpoConfiguration;
import org.icroco.picture.model.Keyword;
import org.springframework.cache.annotation.Cacheable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class InMemoryKeywordManager implements IKeywordManager {
    private final AtomicInteger        incr  = new AtomicInteger(0);
    private final Map<String, Keyword> cache = new ConcurrentHashMap<>();

    @Override
    @Cacheable(cacheNames = SpoConfiguration.CACHE_KEYWORD_RAW, unless = "#result == null")
    public Keyword findOrCreateTag(String name) {
        return cache.computeIfAbsent(name, n -> new Keyword(incr.incrementAndGet(), n));
    }

    @Transactional
    public Set<Keyword> addMissingKw(Collection<Keyword> keywords) {
        var missingKw = keywords.stream()
                                .filter(kw -> kw.id() == null)
                                .peek(kw -> new Keyword(incr.incrementAndGet(), kw.name()))
                                .peek(kw -> cache.put(kw.name(), kw))
                                .collect(Collectors.toSet());

        return Stream.concat(missingKw.stream(), keywords.stream().filter(kw -> kw.id() != null))
                     .collect(Collectors.toSet());
    }


    @Override
    public Collection<Keyword> getAll() {
        return cache.values();
    }
}
