package org.icroco.picture.views.util;

import lombok.experimental.UtilityClass;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class Collections {
    public static <T> StreamEx<List<T>> splitByCore(List<T> values) {
        return StreamEx.ofSubLists(values, Constant.split(values.size()));
    }

    public record SplitResult<T>(int splitCount, EntryStream<Integer, List<T>> values) {}

    public static <T> SplitResult<T> splitByCoreWithIdx(List<T> values) {
        final int splitSize = Constant.split(values.size());
        final var lists     = StreamEx.ofSubLists(values, splitSize).toList();

        return new SplitResult<>(lists.size(), EntryStream.of(lists));
    }

    public static <T> CollectionDiff<T> difference(Collection<T> left, Collection<T> right) {
        return new CollectionDiff<>(CollectionUtils.subtract(right, left),
                                    CollectionUtils.subtract(left, right)
        );
    }

    public static <T> Stream<T> toStream(Iterator<T> iterable) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterable, Spliterator.ORDERED), false);
    }

    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return toStream(iterable.iterator());
    }
}
