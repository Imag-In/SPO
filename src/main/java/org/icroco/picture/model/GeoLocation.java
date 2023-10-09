package org.icroco.picture.model;

import com.drew.lang.annotations.NotNull;
import lombok.Builder;

@Builder
public record GeoLocation(double latitude, double longitude) {
    public static GeoLocation EMPTY_GEO_LOC = new GeoLocation(Double.MIN_VALUE, Double.MIN_VALUE);

    @NotNull
    public String toDMSString() {

        char lonLetter = (longitude > 0) ? 'E' : 'W';
        char latLetter = (latitude > 0) ? 'N' : 'S';
        return com.drew.lang.GeoLocation.decimalToDegreesMinutesSecondsString(Math.abs(longitude))
               + " "
               + lonLetter
               + System.lineSeparator()
               +
               com.drew.lang.GeoLocation.decimalToDegreesMinutesSecondsString(Math.abs(latitude))
               + " "
               + latLetter;
    }

    public boolean isNowhere() {
        return latitude == Double.MIN_VALUE && longitude == Double.MAX_VALUE;
    }

    public boolean isSomewhere() {
        return !isNowhere();
    }
}
