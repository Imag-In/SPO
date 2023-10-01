package org.icroco.picture.views.organize.gallery;

public enum EGalleryClickState {
    GALLERY() {
        @Override
        EGalleryClickState next() {
            return IMAGE;
        }
    },
    IMAGE() {
        @Override
        EGalleryClickState next() {
            return ZOOM;
        }
    },
    ZOOM() {
        @Override
        EGalleryClickState next() {
            return GALLERY;
        }
    };

    abstract EGalleryClickState next();
}
