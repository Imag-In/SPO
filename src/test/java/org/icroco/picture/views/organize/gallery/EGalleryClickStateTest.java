package org.icroco.picture.views.organize.gallery;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class EGalleryClickStateTest {

    @Test
    void should_be_image() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(EGalleryClickState.IMAGE.isImage()).isTrue();
        soft.assertThat(EGalleryClickState.IMAGE_BACK.isImage()).isTrue();
        soft.assertThat(EGalleryClickState.GALLERY.isImage()).isFalse();
        soft.assertThat(EGalleryClickState.ZOOM.isImage()).isFalse();

        soft.assertAll();
    }

}