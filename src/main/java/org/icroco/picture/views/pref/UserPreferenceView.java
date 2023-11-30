package org.icroco.picture.views.pref;

import jakarta.annotation.PostConstruct;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanProperty;
import org.icroco.picture.event.ShowSettingsEvent;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.util.PropertySettings;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.Nodes;
import org.icroco.picture.views.util.SpoPropertyEditorFactory;
import org.reflections.ReflectionUtils;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserPreferenceView implements FxView<DialogPane> {

    private final UserPreferenceService    preferenceService;
    private final SpoPropertyEditorFactory editorFactory;

    private final DialogPane root = new DialogPane();

    @PostConstruct
    final void PostConstruct() {
        root.setId(ViewConfiguration.V_PREFERENCES);
        root.getStyleClass().add(ViewConfiguration.V_PREFERENCES);
        PropertySheet ps = new PropertySheet(getProperties(preferenceService.getUserPreference()));
        getRootContent().setContent(ps);
        getRootContent().getButtonTypes().add(ButtonType.OK);
        ps.setPropertyEditorFactory(editorFactory);
        ps.setMode(PropertySheet.Mode.CATEGORY);
        getRootContent().setPrefSize(500, 400);
    }

    @Override
    public DialogPane getRootContent() {
        return root;
    }

    static ObservableList<PropertySheet.Item> getProperties(UserPreference userPref) {
        ObservableList<PropertySheet.Item> items = FXCollections.observableArrayList();

        addProperties(items, userPref.getMainWindow());
        addProperties(items, userPref.getCollection());

        return items;
    }

    static void addProperties(ObservableList<PropertySheet.Item> items,
                              Object bean) {
        try {
            var fields = ReflectionUtils.getFields(bean.getClass(), ReflectionUtils.withAnnotation(PropertySettings.class))
                                        .stream().collect(Collectors.toMap(Field::getName, Function.identity()));
            var beanInfo = Introspector.getBeanInfo(bean.getClass());
            for (PropertyDescriptor p : beanInfo.getPropertyDescriptors()) {
                BeanProperty property = new BeanProperty(bean, p);
                Optional.ofNullable(fields.get(p.getName()))
                        .ifPresent(field -> {
                            var settings = field.getAnnotation(PropertySettings.class);
                            var category = settings.category().isBlank() ? bean.getClass().getSimpleName() : settings.category();
                            p.setValue(BeanProperty.CATEGORY_LABEL_KEY, settings.category());
                            property.setEditable(settings.isEditable());
                            p.setPreferred(settings.isFavorite());
                            if (!settings.displayName().isBlank()) {
                                p.setDisplayName(settings.displayName());
                            }
                            if (!settings.description().isBlank()) {
                                p.setShortDescription(settings.description());
                            }
                            if (settings.editor().length > 0) {
                                p.setPropertyEditorClass(settings.editor()[0]);
                            }
                            items.add(property);
                        });
            }

        } catch (IntrospectionException e) {
            throw new RuntimeException("Should not occured", e);
        }
    }

    @FxEventListener
    public void showSettings(ShowSettingsEvent event) {
        final var dialog = new Dialog<Void>();
        dialog.setResizable(true);

//        dialog.setWidth(preferenceService.getUserPreference().getSettings().getDimension().width());
//        dialog.setHeight(preferenceService.getUserPreference().getSettings().getDimension().height());
        dialog.setDialogPane(getRootContent());
        getRootContent().setPrefSize(preferenceService.getUserPreference().getSettings().getDimension().width(),
                                     preferenceService.getUserPreference().getSettings().getDimension().height());
//        dialog.getDialogPane().applyCss();
        dialog.getDialogPane().layout();
//        dialog.setWidth(preferenceService.getUserPreference().getSettings().getDimension().width());
//        dialog.setHeight(preferenceService.getUserPreference().getSettings().getDimension().height());
        dialog.setOnCloseRequest(_ -> {
            preferenceService.getUserPreference()
                             .getSettings()
//                             .setDimension(new Dimension(dialog.getWidth(), dialog.getHeight()));});
                             .setDimension(new Dimension(getRootContent().getWidth(), getRootContent().getHeight()));
        });
        Nodes.show(dialog, event.getScene());
    }

    public static void centerChildWindowOnStage(Stage stage, Scene ownerScene) {

        if (ownerScene == null) {
            return;
        }

        double x = stage.getX();
        double y = stage.getY();

        // Firstly we need to force CSS and layout to happen, as the dialogPane
        // may not have been shown yet (so it has no dimensions)
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();

        final double titleBarHeight = ownerScene.getY();

        // because Stage does not seem to centre itself over its owner, we
        // do it here.

        // then we can get the dimensions and position the dialog appropriately.
        final double dialogWidth  = stage.getScene().getRoot().prefWidth(-1);
        final double dialogHeight = stage.getScene().getRoot().prefHeight(dialogWidth);

        final double ownerWidth  = ownerScene.getRoot().prefWidth(-1);
        final double ownerHeight = ownerScene.getRoot().prefHeight(ownerWidth);

        if (dialogWidth < ownerWidth) {
            x = ownerScene.getX() + (ownerScene.getWidth() / 2.0) - (dialogWidth / 2.0);
        } else {
            x = ownerScene.getX();
            stage.setWidth(dialogWidth);
        }

        if (dialogHeight < ownerHeight) {
            y = ownerScene.getY() + titleBarHeight / 2.0 + (ownerScene.getHeight() / 2.0) - (dialogHeight / 2.0);
        } else {
            y = ownerScene.getY();
        }

        stage.setX(x);
        stage.setY(y);
    }
}
