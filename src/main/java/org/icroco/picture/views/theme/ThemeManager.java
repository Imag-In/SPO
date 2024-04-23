/* SPDX-License-Identifier: MIT */

package org.icroco.picture.views.theme;

import atlantafx.base.theme.Theme;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import org.icroco.picture.util.Resources;
import org.icroco.picture.views.StageRepository;
import org.icroco.picture.views.util.JColor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icroco.picture.util.Resources.getResource;

@Component
public final class ThemeManager {
    static final String   DUMMY_STYLESHEET = getResource("/styles/empty.css").toString();
    static final String[] APP_STYLESHEETS  = new String[] {
            Resources.resolve("/styles/empty.css"),
            Resources.resolve("/styles/index.css")
    };

    private static final PseudoClass DARK        = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass USER_CUSTOM = PseudoClass.getPseudoClass("user-custom");
//    private static final EventBus    EVENT_BUS   = DefaultEventBus.getInstance();

    public static final String        DEFAULT_FONT_FAMILY_NAME = "fonts/Inter";
    public static final int           DEFAULT_FONT_SIZE        = 14;
    public static final int           DEFAULT_ZOOM             = 100;
    public static final AccentColor   DEFAULT_ACCENT_COLOR     = null;
    public static final List<Integer> SUPPORTED_FONT_SIZE      = IntStream.range(8, 29).boxed().collect(Collectors.toList());
    public static final List<Integer> SUPPORTED_ZOOM           = List.of(50, 75, 80, 90, 100, 110, 125, 150, 175, 200);

    private final Map<String, String> customCSSDeclarations = new LinkedHashMap<>(); // -fx-property | value;
    private final Map<String, String> customCSSRules        = new LinkedHashMap<>(); // .foo | -fx-property: value;


    @Getter
    private final ThemeRepository themeRepository;
    private final StageRepository stageRepository;
//    @Getter
//    private       Scene           scene;

    private SamplerTheme currentTheme = null;
    @Getter
    private String       fontFamily   = DEFAULT_FONT_FAMILY_NAME;
    @Getter
    private int          fontSize     = DEFAULT_FONT_SIZE;
    @Getter
    private int          zoom         = DEFAULT_ZOOM;
    @Getter
    private AccentColor  accentColor  = DEFAULT_ACCENT_COLOR;

    public ThemeManager(ThemeRepository themeRepository, StageRepository stageRepository) {
        this.themeRepository = themeRepository;
        this.stageRepository = stageRepository;

        /* w  ww. j a va 2  s.  c o m*/
        stageRepository.getStages().addListener((SetChangeListener<Stage>) change -> {
            if (change.wasAdded()) {
                var stage = change.getElementAdded();
                applyTheme(stage.getScene());
            }
        });
    }

    public SamplerTheme getTheme() {
        return currentTheme;
    }

    public SamplerTheme getDefaultTheme() {
        return getThemeRepository().getDefault();
    }

    /**
     * See {@link SamplerTheme}.
     */
    public void setTheme(SamplerTheme theme) {
        Objects.requireNonNull(theme);

        currentTheme = theme;
        Application.setUserAgentStylesheet(Objects.requireNonNull(theme.getUserAgentStylesheet()));
        stageRepository.getStages()
                       .stream()
                       .map(Stage::getScene)
                       .forEach(this::applyTheme);

    }

    private void applyTheme(Scene scene) {
        if (currentTheme != null) {
            animateThemeChange(scene, Duration.millis(750));
            scene.getStylesheets().setAll(currentTheme.getAllStylesheets());
            scene.getRoot().pseudoClassStateChanged(DARK, currentTheme.isDarkMode());
            // remove user CSS customizations and reset accent on theme change
            resetAccentColor(scene);
            resetCustomCSS(scene);
        }
    }

    public void setFontFamily(Scene scene, String fontFamily) {
        Objects.requireNonNull(fontFamily);
        setCustomDeclaration("-fx-font-family", STR."\"\{fontFamily}\"");

        this.fontFamily = fontFamily;

        reloadCustomCSS(scene);
//        EVENT_BUS.publish(new ThemeEvent(EventType.FONT_CHANGE));
    }

    public boolean isDefaultFontFamily() {
        return Objects.equals(DEFAULT_FONT_FAMILY_NAME, getFontFamily());
    }

    public void setFontSize(Scene scene, int size) {
        if (!SUPPORTED_FONT_SIZE.contains(size)) {
            throw new IllegalArgumentException(
                    String.format("Font size must in the range %d-%dpx. Actual value is %d.",
                                  SUPPORTED_FONT_SIZE.getFirst(),
                                  SUPPORTED_FONT_SIZE.getLast(),
                                  size
                    ));
        }

        setCustomDeclaration("-fx-font-size", STR."\{size}px");
        setCustomRule(".ikonli-font-icon", String.format("-fx-icon-size: %dpx;", size + 2));

        this.fontSize = size;

        var rawZoom = (int) Math.ceil((size * 1.0 / DEFAULT_FONT_SIZE) * 100);
        this.zoom = SUPPORTED_ZOOM.stream()
                                  .min(Comparator.comparingInt(i -> Math.abs(i - rawZoom)))
                                  .orElseThrow(NoSuchElementException::new);

        reloadCustomCSS(scene);
//        EVENT_BUS.publish(new ThemeEvent(EventType.FONT_CHANGE));
    }

    public boolean isDefaultSize() {
        return DEFAULT_FONT_SIZE == fontSize;
    }

    public void setZoom(Scene scene, int zoom) {
        if (!SUPPORTED_ZOOM.contains(zoom)) {
            throw new IllegalArgumentException(
                    String.format("Zoom value must one of %s. Actual value is %d.", SUPPORTED_ZOOM, zoom)
            );
        }

        setFontSize(scene, (int) Math.ceil(zoom != 100 ? (DEFAULT_FONT_SIZE * zoom) / 100.0f : DEFAULT_FONT_SIZE));
        this.zoom = zoom;
    }

    public void setAccentColor(Scene scene, AccentColor color) {
        Objects.requireNonNull(color);

        animateThemeChange(scene, Duration.millis(350));

        if (accentColor != null) {
            scene.getRoot().pseudoClassStateChanged(accentColor.pseudoClass(), false);
        }

        scene.getRoot().pseudoClassStateChanged(color.pseudoClass(), true);
        this.accentColor = color;

//        EVENT_BUS.publish(new ThemeEvent(EventType.COLOR_CHANGE));
    }

    public void resetAccentColor(Scene scene) {
        animateThemeChange(scene, Duration.millis(350));

        if (accentColor != null) {
            scene.getRoot().pseudoClassStateChanged(accentColor.pseudoClass(), false);
            accentColor = null;
        }

//        EVENT_BUS.publish(new ThemeEvent(EventType.COLOR_CHANGE));
    }

    public void setNamedColors(Scene scene, Map<String, Color> colors) {
        Objects.requireNonNull(colors).forEach(this::setOrRemoveColor);
        reloadCustomCSS(scene);
//        EVENT_BUS.publish(new ThemeEvent(EventType.COLOR_CHANGE));
    }

    public void unsetNamedColors(Scene scene, String... colors) {
        for (String c : colors) {
            setOrRemoveColor(c, null);
        }
        reloadCustomCSS(scene);
//        EVENT_BUS.publish(new ThemeEvent(EventType.COLOR_CHANGE));
    }

    public void resetAllChanges(Scene scene) {
        resetCustomCSS(scene);
//        EVENT_BUS.publish(new ThemeEvent(EventType.THEME_CHANGE));
    }

    public HighlightJSTheme getMatchingSourceCodeHighlightTheme(Theme theme) {
        Objects.requireNonNull(theme);
        if ("Nord Light".equals(theme.getName())) {
            return HighlightJSTheme.nordLight();
        }
        if ("Nord Dark".equals(theme.getName())) {
            return HighlightJSTheme.nordDark();
        }
        if ("Dracula".equals(theme.getName())) {
            return HighlightJSTheme.dracula();
        }
        return theme.isDarkMode() ? HighlightJSTheme.githubDark() : HighlightJSTheme.githubLight();
    }

    ///////////////////////////////////////////////////////////////////////////

    private void setCustomDeclaration(String property, String value) {
        customCSSDeclarations.put(property, value);
    }

    private void removeCustomDeclaration(String property) {
        customCSSDeclarations.remove(property);
    }

    private void setCustomRule(String selector, String rule) {
        customCSSRules.put(selector, rule);
    }

    @SuppressWarnings("unused")
    private void removeCustomRule(String selector) {
        customCSSRules.remove(selector);
    }

    private void setOrRemoveColor(String colorName, Color color) {
        Objects.requireNonNull(colorName);
        if (color != null) {
            setCustomDeclaration(colorName, JColor.color((float) color.getRed(),
                                                         (float) color.getGreen(),
                                                         (float) color.getBlue(),
                                                         (float) color.getOpacity()).getColorHexWithAlpha()
            );
        } else {
            removeCustomDeclaration(colorName);
        }
    }

    private void animateThemeChange(Scene scene, Duration duration) {
        Image snapshot = scene.snapshot(null);
        Pane  root     = (Pane) scene.getRoot();

        ImageView imageView = new ImageView(snapshot);
        root.getChildren().add(imageView); // add snapshot on top

        var transition = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_OUT)),
                new KeyFrame(duration, new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT))
        );
        transition.setOnFinished(e -> root.getChildren().remove(imageView));
        transition.play();
    }

    private void reloadCustomCSS(Scene scene) {
        Objects.requireNonNull(scene);
        StringBuilder css = new StringBuilder();

        css.append(".root:");
        css.append(USER_CUSTOM.getPseudoClassName());
        css.append(" {\n");
        customCSSDeclarations.forEach((k, v) -> {
            css.append("\t");
            css.append(k);
            css.append(":\s");
            css.append(v);
            css.append(";\n");
        });
        css.append("}\n");

        customCSSRules.forEach((k, v) -> {
            // custom CSS is applied to the body,
            // thus it has a preference over accent color
            css.append(".body:");
            css.append(USER_CUSTOM.getPseudoClassName());
            css.append(" ");
            css.append(k);
            css.append(" {");
            css.append(v);
            css.append("}\n");
        });

        scene.getRoot().getStylesheets().removeIf(uri -> uri.startsWith("data:text/css"));
        scene.getRoot().getStylesheets().add(
                "data:text/css;base64," + Base64.getEncoder().encodeToString(css.toString().getBytes(UTF_8))
        );
        scene.getRoot().pseudoClassStateChanged(USER_CUSTOM, true);
    }

    public void resetCustomCSS(Scene scene) {
        customCSSDeclarations.clear();
        customCSSRules.clear();
        scene.getRoot().pseudoClassStateChanged(USER_CUSTOM, false);
    }
}
