package org.icroco.picture.ui.util;

import lombok.experimental.UtilityClass;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import java.util.List;

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
}
