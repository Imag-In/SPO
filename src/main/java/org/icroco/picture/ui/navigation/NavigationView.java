package org.icroco.picture.ui.navigation;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.util.widget.FxUtil;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class NavigationView extends HBox {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    Label importLbl   = new Label();
    Label organizeLbl = new Label();
    Label peopleLbl   = new Label();
    Label exportLbl   = new Label();
    private final ObjectProperty<Label> selectedTab = new SimpleObjectProperty<>();

    public NavigationView() {
        getStyleClass().add("tabs");
        setAlignment(Pos.CENTER);
        setPadding(new Insets(0, 0, 10, 0));

        organizeLbl.setText("Organize");
        importLbl.setDisable(false);
        importLbl.setText("Import");
        importLbl.setDisable(true);
        peopleLbl.setText("People");
        peopleLbl.setDisable(true);
        exportLbl.setText("Export");
        exportLbl.setDisable(true);

        organizeLbl.pseudoClassStateChanged(SELECTED, false);

        initTabLabel(importLbl);
        initTabLabel(organizeLbl);
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

            if (old != null) {
                old.pseudoClassStateChanged(SELECTED, false);
            }
            if (val != null) {
                val.pseudoClassStateChanged(SELECTED, true);
            }
        });

        selectedTab.set(organizeLbl);
        var settings = new Button(null, new FontIcon(Material2OutlinedMZ.SETTINGS));
        settings.setTooltip(new Tooltip("Settings"));
        FxUtil.styleCircleButton(settings).setOnAction(this::openSettings);

        getChildren().addAll(new Spacer(), importLbl, organizeLbl, peopleLbl, exportLbl, new Spacer(), settings);

    }

    private void openSettings(ActionEvent e) {
        log.info("Open Settings");
    }

    private Label initTabLabel(Label label) {
        label.setOnMouseClicked(e -> selectedTab.set(label));
//        label.setPrefWidth(120);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll(Styles.TITLE_2);
        label.setPadding(new Insets(0, 10, 0, 10));

        return label;
    }

}
