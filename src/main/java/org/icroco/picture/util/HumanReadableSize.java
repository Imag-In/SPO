package org.icroco.picture.util;

import java.text.DecimalFormat;

// https://www.baeldung.com/java-human-readable-byte-size
public class HumanReadableSize {
    public static final double
            KILO = 1000L, // 1000 power 1 (10 power 3)
            KIBI = 1024L, // 1024 power 1 (2 power 10)
            MEGA = KILO * KILO, // 1000 power 2 (10 power 6)
            MEBI = KIBI * KIBI, // 1024 power 2 (2 power 20)
            GIGA = MEGA * KILO, // 1000 power 3 (10 power 9)
            GIBI = MEBI * KIBI, // 1024 power 3 (2 power 30)
            TERA = GIGA * KILO, // 1000 power 4 (10 power 12)
            TEBI = GIBI * KIBI, // 1024 power 4 (2 power 40)
            PETA = TERA * KILO, // 1000 power 5 (10 power 15)
            PEBI = TEBI * KIBI, // 1024 power 5 (2 power 50)
            EXA  = PETA * KILO, // 1000 power 6 (10 power 18)
            EXBI = PEBI * KIBI; // 1024 power 6 (2 power 60)

    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static String binaryBased(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Argument cannot be negative");
        } else if (size < KIBI) {
            return df.format(size).concat("B");
        } else if (size < MEBI) {
            return df.format(size / KIBI).concat("KiB");
        } else if (size < GIBI) {
            return df.format(size / MEBI).concat("MiB");
        } else if (size < TEBI) {
            return df.format(size / GIBI).concat("GiB");
        } else if (size < PEBI) {
            return df.format(size / TEBI).concat("TiB");
        } else if (size < EXBI) {
            return df.format(size / PEBI).concat("PiB");
        } else {
            return df.format(size / EXBI).concat("EiB");
        }
    }

    public static String decimalBased(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Argument cannot be negative");
        } else if (size < KILO) {
            return df.format(size).concat("B");
        } else if (size < MEGA) {
            return df.format(size / KILO).concat("KB");
        } else if (size < GIGA) {
            return df.format(size / MEGA).concat("MB");
        } else if (size < TERA) {
            return df.format(size / GIGA).concat("GB");
        } else if (size < PETA) {
            return df.format(size / TERA).concat("TB");
        } else if (size < EXA) {
            return df.format(size / PETA).concat("PB");
        } else {
            return df.format(size / EXA).concat("EB");
        }
    }
}