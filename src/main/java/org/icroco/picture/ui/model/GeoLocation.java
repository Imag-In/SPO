package org.icroco.picture.ui.model;

import com.drew.lang.annotations.NotNull;

public record GeoLocation(double latitude, double longitude) {
    public static GeoLocation EMPTY_GEO_LOC = new GeoLocation(0, 0);

    @NotNull
    public String toDMSString() {
        return com.drew.lang.GeoLocation.decimalToDegreesMinutesSecondsString(latitude) + "; " +
               com.drew.lang.GeoLocation.decimalToDegreesMinutesSecondsString(longitude);
    }
}
