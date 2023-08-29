/* SPDX-License-Identifier: MIT */

package org.icroco.picture.ui.util;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.ImagInApp;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@Slf4j
public final class Resources {

    public static final String MODULE_DIR = "/";

    public static InputStream getResourceAsStream(String resource) {
        String path = resolve(resource);
        return Objects.requireNonNull(
                ImagInApp.class.getResourceAsStream(resolve(path)),
                "Resource not found: " + path
        );
    }

    public static URI getResource(String resource) {
        String path = resolve(resource);
        URL    url  = Objects.requireNonNull(ImagInApp.class.getResource(resolve(path)), "Resource not found: " + path);
        return URI.create(url.toExternalForm());
    }

    public static String resolve(String resource) {
        Objects.requireNonNull(resource);
        return resource.startsWith("/") ? resource : MODULE_DIR + resource;
    }

    public static String getPropertyOrEnv(String propertyKey, String envKey) {
        return System.getProperty(propertyKey, System.getenv(envKey));
    }

    public static Preferences getPreferences() {
        var p = Preferences.userNodeForPackage(ImagInApp.class).node("imag-in/spo");
        return p;
    }

    public static void printPreferences(Preferences pref, String prefix) {
        try {
            Arrays.stream(pref.keys())
                  .forEach(s -> log.info("{}{}='{}'", prefix, s, pref.get(s, "null")));
            Arrays.stream(pref.childrenNames())
                  .forEach(c -> {
                      log.info("{}{}:", prefix, c);
                      printPreferences(pref.node(c), prefix + "    ");
                  });
        }
        catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
