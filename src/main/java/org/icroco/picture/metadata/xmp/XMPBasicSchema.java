package org.icroco.picture.metadata.xmp;

import org.apache.xmlgraphics.xmp.Metadata;

public class XMPBasicSchema extends org.apache.xmlgraphics.xmp.schemas.XMPBasicSchema {
    public static XMPBasicAdapter getAdapter(Metadata meta) {
        return new XMPBasicAdapter(meta, NAMESPACE);
    }

    public static class XMPBasicAdapter extends org.apache.xmlgraphics.xmp.schemas.XMPBasicAdapter {

        public XMPBasicAdapter(Metadata meta, String namespace) {
            super(meta, namespace);
        }

        public void setRating(int value) {
            setValue("Rating", String.valueOf(value));
        }
    }
}
