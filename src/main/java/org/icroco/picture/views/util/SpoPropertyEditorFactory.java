package org.icroco.picture.views.util;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import org.icroco.picture.util.Constant;
import org.icroco.picture.views.theme.SamplerTheme;
import org.icroco.picture.views.theme.ThemeManager;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SpoPropertyEditorFactory extends DefaultPropertyEditorFactory {
    private final ThemeManager themeManager;

    public PropertyEditor<?> call(PropertySheet.Item item) {
        var value = item.getValue();
        if (value instanceof SamplerTheme) {
            var ediror = Editors.createChoiceEditor(item, themeManager.getThemeRepository().getAll());
            ComboBox<SamplerTheme> editor = (ComboBox<SamplerTheme>) ediror.getEditor();
            editor.setConverter(new StringConverter<>() {
                @Override
                public String toString(SamplerTheme object) {
                    return object.getName();
                }

                @Override
                public SamplerTheme fromString(String string) {
                    return themeManager.getThemeRepository()
                                       .getAll()
                                       .stream()
                                       .filter(st -> st.getName().equals(string))
                                       .findFirst()
                                       .orElseGet(themeManager::getDefaultTheme);
                }
            });
            editor.getSelectionModel()
                  .selectedItemProperty()
                  .addListener((_, _, newValue) -> themeManager.setTheme(newValue));
            return ediror;

        } else if (value instanceof Locale) {
            var              ediror = Editors.createChoiceEditor(item, Constant.getSupportedLocales());
            ComboBox<Locale> editor = (ComboBox<Locale>) ediror.getEditor();
            editor.setConverter(new StringConverter<>() {
                @Override
                public String toString(Locale object) {
                    return object.getDisplayName();
                }

                @Override
                public Locale fromString(String string) {
                    return Locale.of(string);
                }
            });
//            editor.getSelectionModel()
//                  .selectedItemProperty()
//                  .addListener((_, _, newValue) -> );
            return ediror;
        } else {
            return super.call(item);
        }
    }
}
