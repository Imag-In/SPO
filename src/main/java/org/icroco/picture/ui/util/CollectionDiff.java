package org.icroco.picture.ui.util;

import java.util.Collection;

public record CollectionDiff<T>(Collection<T> leftMissing, Collection<T> rightMissing) {
    public boolean isEmpty() {
        return leftMissing.isEmpty() && rightMissing.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
