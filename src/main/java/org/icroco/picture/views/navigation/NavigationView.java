package org.icroco.picture.views.navigation;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.ShowOrganizeEvent;
import org.icroco.picture.event.ShowSettingsEvent;
import org.icroco.picture.util.I18N;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.compare.DiffWindow;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.widget.FxUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class NavigationView implements FxView<HBox> {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final SimpleStringProperty currentView;

    private final HBox  root = new HBox();
    private final Label importLbl;
    private final Label organizeLbl;
    private final Label repairLbl;
    private final Label peopleLbl;
    private final Label exportLbl;
    private final ObjectProperty<Label> selectedTab = new SimpleObjectProperty<>();


    public NavigationView(@Qualifier(ViewConfiguration.CURRENT_VIEW)
                          SimpleStringProperty currentView,
                          DiffWindow diffWindow,
                          UserPreferenceService preferenceService,
                          TaskService taskService,
                          I18N i18N) {
        this.currentView = currentView;
        root.setId(ViewConfiguration.V_NAVIGATION);
        root.getStyleClass().add(ViewConfiguration.V_NAVIGATION);
        root.getStyleClass().add("tabs");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(5, 0, 5, 0));

        organizeLbl = i18N.labelForKey("navBar.organize");
        importLbl = i18N.labelForKey("navBar.import");
        repairLbl = i18N.labelForKey("navBar.tools");
        peopleLbl = i18N.labelForKey("navBar.people");
        peopleLbl.setDisable(true);
        exportLbl = i18N.labelForKey("navBar.export");
        exportLbl.setDisable(true);

        organizeLbl.pseudoClassStateChanged(SELECTED, false);

        initTabLabel(importLbl);
        initTabLabel(organizeLbl);
        initTabLabel(repairLbl);
        initTabLabel(peopleLbl);
        initTabLabel(exportLbl);

        selectedTab.addListener((_, old, val) -> {
//            if (val == codeTab) {
//                stateToggle.setDisable(true);
//                content.getChildren().setAll(snippet.render());
//            } else {
//                stateToggle.setDisable(false);
//                content.getChildren().setAll(preview);
//            }
            if (val == importLbl) {
                currentView.set(ViewConfiguration.V_IMPORT);
            } else if (val == organizeLbl) {
                currentView.set(ViewConfiguration.V_ORGANIZE);
            } else if (val == repairLbl) {
                currentView.set(ViewConfiguration.V_REPAIR);
            }

            if (old != null) {
                old.pseudoClassStateChanged(SELECTED, false);
            }
            if (val != null) {
                val.pseudoClassStateChanged(SELECTED, true);
            }
        });

        selectedTab.set(organizeLbl);

        var settingsIcon = FontIcon.of(Material2OutlinedMZ.SETTINGS);
        settingsIcon.setId("settings");
        settingsIcon.getStyleClass().add("button-top-bar");
        var settings = new Button(null, settingsIcon);
        settings.setTooltip(new Tooltip("Settings"));
        settings.setDisable(false);
        settings.getStyleClass().add(Styles.LARGE);
        FxUtil.styleCircleButton(settings).setOnAction(this::openSettings);
        settings.setOnMouseClicked(_ -> taskService.sendEvent(ShowSettingsEvent.builder()
                                                                               .scene(getRootContent().getScene())
                                                                               .source(this)
                                                                               .build()));
        var photoDiffIcon = FontIcon.of(MaterialDesignC.COMPARE);
        var photoDiff     = new Button(null, photoDiffIcon);
        photoDiffIcon.getStyleClass().add("button-top-bar");
        photoDiff.setOnMouseClicked(event -> diffWindow.show());

//        photoDiff.setTooltip(new Tooltip("Settings"));
//        photoDiff.setDisable(true);
        photoDiff.getStyleClass().add(Styles.LARGE);
        FxUtil.styleCircleButton(photoDiff); //.setOnAction(this::openSettings);

        root.getChildren().addAll(new Spacer(), importLbl, organizeLbl, repairLbl, peopleLbl, exportLbl, new Spacer(), photoDiff, settings);

    }

    private void openSettings(ActionEvent e) {
        log.info("Open Settings");
    }

    private Label initTabLabel(Label label) {
        label.setOnMouseClicked(e -> selectedTab.set(label));
//        label.setPrefWidth(120);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add(Styles.TITLE_2);
        label.setPadding(new Insets(0, 10, 0, 10));

        return label;
    }

    @FxEventListener
    public void importDir(ImportDirectoryEvent event) {
        selectedTab.setValue(importLbl);
    }

    @FxEventListener
    public void importDir(ShowOrganizeEvent event) {
        selectedTab.setValue(organizeLbl);
    }

    @Override
    public HBox getRootContent() {
        return root;
    }
}
