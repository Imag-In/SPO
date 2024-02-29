package org.icroco.picture.model;

import com.drew.lang.annotations.NotNull;
import lombok.Builder;

@Builder
public record GeoLocation(double latitude, double longitude) {
    public static GeoLocation EMPTY_GEO_LOC = new GeoLocation(190, 190);

    public GeoLocation {
        if (latitude < -90D || latitude > 90) {
            latitude = 190D;
        }
        if (longitude < -180D || longitude > 190D) {
            longitude = 190D;
        }
    }

    @NotNull
    public String toDMSString() {

        char lonLetter = (longitude > 0) ? 'E' : 'W';
        char latLetter = (latitude > 0) ? 'N' : 'S';
        return STR."\{com.drew.lang.GeoLocation.decimalToDegreesMinutesSecondsString(Math.abs(longitude))} \{lonLetter}\{System.lineSeparator()}\{com.drew.lang.GeoLocation.decimalToDegreesMinutesSecondsString(
                Math.abs(latitude))} \{latLetter}";
    }

    public boolean isNowhere() {
        return Math.abs(latitude) > 90D || Math.abs(longitude) > 180D;
    }

    public boolean isSomewhere() {
        return !isNowhere();
    }
}
