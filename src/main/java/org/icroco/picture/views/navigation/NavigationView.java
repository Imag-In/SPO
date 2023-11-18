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
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.widget.FxUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class NavigationView implements FxView<HBox> {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final SimpleStringProperty currentView;

    private final HBox  root        = new HBox();
    private final Label importLbl   = new Label();
    private final Label organizeLbl = new Label();
    private final Label repairLbl   = new Label();
    private final Label peopleLbl   = new Label();
    private final Label exportLbl   = new Label();
    private final ObjectProperty<Label> selectedTab = new SimpleObjectProperty<>();

    public NavigationView(@Qualifier(ViewConfiguration.CURRENT_VIEW)
                          SimpleStringProperty currentView) {
        this.currentView = currentView;
        root.setId(ViewConfiguration.V_NAVIGATION);
        root.getStyleClass().add(ViewConfiguration.V_NAVIGATION);
        root.getStyleClass().add("tabs");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0, 0, 10, 0));

        organizeLbl.setText("Organize");
        importLbl.setDisable(false);
        importLbl.setText("Import");
        repairLbl.setText("Repair");
        repairLbl.setDisable(true);
        peopleLbl.setText("People");
        peopleLbl.setDisable(true);
        exportLbl.setText("Export");
        exportLbl.setDisable(true);

        organizeLbl.pseudoClassStateChanged(SELECTED, false);

        initTabLabel(importLbl);
        initTabLabel(organizeLbl);
        initTabLabel(repairLbl);
        initTabLabel(peopleLbl);
        initTabLabel(exportLbl);

        selectedTab.addListener((obs, old, val) -> {
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
            }

            if (old != null) {
                old.pseudoClassStateChanged(SELECTED, false);
            }
            if (val != null) {
                val.pseudoClassStateChanged(SELECTED, true);
            }
        });

        selectedTab.set(organizeLbl);
        FontIcon settingsIcon = new FontIcon(Material2OutlinedMZ.SETTINGS);
        settingsIcon.setIconSize(64);
        settingsIcon.getStyleClass().add(Styles.LARGE);
//        settingsIcon.setIconSize(24);

        var settings = new Button(null, settingsIcon);
        settings.setTooltip(new Tooltip("Settings"));
        settings.setDisable(true);
        FxUtil.styleCircleButton(settings).setOnAction(this::openSettings);

        root.getChildren().addAll(new Spacer(), importLbl, organizeLbl, repairLbl, peopleLbl, exportLbl, new Spacer(), settings);

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
