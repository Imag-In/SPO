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
            return IMAGE_BACK;
        }
    },

    IMAGE_BACK() {
        @Override
        EGalleryClickState next() {
            return ZOOM;
        }
    };

    abstract EGalleryClickState next();

    public boolean isImage() {
        return this == IMAGE || this == IMAGE_BACK;
    }
}
