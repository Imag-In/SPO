package org.icroco.picture.util;

import atlantafx.base.controls.ToggleSwitch;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

/**
 * I18N utility class.
 *
 * @author P.J. Meisch (pj.meisch@sothawo.com).
 */
@Component
@Slf4j
public final class I18N {

    public static final String SPO_ASSETS_LANGUAGES_MESSAGES = "spo/assets/languages/messages";
    /**
     * the current selected Locale.
     */
    private final ObjectProperty<Locale> locale;

    public I18N(final UserPreferenceService preferenceService) {
        locale = preferenceService.getUserPreference().getGeneral().localeProperty();
        locale.addListener((_, _, newValue) -> log.info("Language: '{}'", newValue));
    }

    private Locale getLocale() {
        return locale.get();
    }

//    public static void setLocale(Locale locale) {
//        localeProperty().set(locale);
//        Locale.setDefault(locale);
//    }

    public ReadOnlyObjectProperty<Locale> localeProperty() {
        return locale;
    }

    /**
     * gets the string with the given key from the resource bundle for the current locale and uses it as first argument
     * to MessageFormat.format, passing in the optional args and returning the result.
     *
     * @param key  message key
     * @param args optional arguments for the message
     * @return localized formatted string
     */
    public String get(final String key, final Object... args) {
        try {
            var bundle = ResourceBundle.getBundle(SPO_ASSETS_LANGUAGES_MESSAGES, getLocale());
            var            text   = bundle.getString(key);
            return MessageFormat.format(text, args);
        } catch (MissingResourceException exception) {
            var bundle = ResourceBundle.getBundle(SPO_ASSETS_LANGUAGES_MESSAGES, Locale.ENGLISH);
            log.error("Bundle key:{}, salue: {}", key, bundle.containsKey(key));
            return MessageFormat.format(bundle.getString(key), args);
        }
    }

    /**
     * creates a String binding to a localized String for the given message bundle key
     *
     * @param key key
     * @return String binding
     */
    public StringBinding createStringBinding(final String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), locale);
    }

    /**
     * creates a String Binding to a localized String that is computed by calling the given func
     *
     * @param func function called on every change
     * @return StringBinding
     */
    public StringBinding createStringBinding(Callable<String> func) {
        return Bindings.createStringBinding(func, locale);
    }

    /**
     * creates a bound Label whose value is computed on language change.
     *
     * @param func the function to compute the value
     * @return Label
     */
    public Label labelForValue(Callable<String> func) {
        Label label = new Label();
        label.textProperty().bind(createStringBinding(func));
        return label;
    }

    public Label labelForKey(final String key, final Object... args) {
        Label label = new Label();
        label.textProperty().bind(createStringBinding(key, args));
        return label;
    }

    public ToggleSwitch toggleSwitchForKey(final String key, final Object... args) {
        var node = new ToggleSwitch();
        node.textProperty().bind(createStringBinding(key, args));
        return node;
    }

    /**
     * creates a bound Button for the given resourcebundle key
     *
     * @param key  ResourceBundle key
     * @param args optional arguments for the message
     * @return Button
     */
    public Button buttonForKey(final String key, final Object... args) {
        Button button = new Button();
        button.textProperty().bind(createStringBinding(key, args));
        return button;
    }

    /**
     * creates a bound Tooltip for the given resourcebundle key
     *
     * @param key  ResourceBundle key
     * @param args optional arguments for the message
     * @return Label
     */
    public Tooltip tooltipForKey(final String key, final Object... args) {
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(createStringBinding(key, args));
        return tooltip;
    }

    public void bindPrompt(final TextField textField, final String key, final Object... args) {
        textField.promptTextProperty().bind(createStringBinding(key, args));
    }

    public void bindText(final StringProperty stringProperty, final String key, final Object... args) {
        stringProperty.bind(createStringBinding(key, args));
    }

}