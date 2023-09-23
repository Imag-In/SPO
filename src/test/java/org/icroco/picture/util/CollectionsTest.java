package org.icroco.picture.util;

import org.assertj.core.api.SoftAssertions;
import org.icroco.picture.views.util.CollectionDiff;
import org.icroco.picture.views.util.Collections;
import org.junit.jupiter.api.Test;

import java.util.List;

class CollectionsTest {

    @Test
    void should_get_difference() {
        List<String> left  = List.of("A", "B", "C", "Z");
        List<String> right = List.of("Z", "B", "C", "D", "E");

        CollectionDiff<String> difference = Collections.difference(left, right);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(difference.isEmpty()).isFalse();
        soft.assertThat(difference.leftMissing()).containsExactly("D", "E");
        soft.assertThat(difference.rightMissing()).containsExactly("A");

        soft.assertAll();
    }

}