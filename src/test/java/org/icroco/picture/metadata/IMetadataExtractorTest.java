package org.icroco.picture.metadata;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class IMetadataExtractorTest {

    @Test
    void should_not_equals() {
        var one = new IMetadataExtractor.DirectorEntryKey(-3, null);
        var two = new IMetadataExtractor.DirectorEntryKey(0, null);

        Assertions.assertThat(one).isNotEqualByComparingTo(two);

    }

}