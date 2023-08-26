package org.icroco.picture.ui.navigation;

import atlantafx.base.theme.Styles;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;


@Slf4j
@FxViewBinding(id = "navigation", fxmlLocation = "nav.fxml")
@RequiredArgsConstructor
public class NavigationController extends FxInitOnce {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    @FXML
    HBox container;
    public        Label                 importLbl;
    public        Label                 organizeLbl;
    public        Label                 peopleLbl;
    public        Label                 exportLbl;
    private final ObjectProperty<Label> selectedTab = new SimpleObjectProperty<>();

    @Override
    protected void initializedOnce() {
        container.getStyleClass().add("navigation");

        importLbl.setDisable(true);
        peopleLbl.setDisable(true);
        exportLbl.setDisable(true);

        initTabLabel(importLbl);
        initTabLabel(organizeLbl);
        initTabLabel(peopleLbl);
        initTabLabel(exportLbl);

        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("tabs");
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

    }

    private Label initTabLabel(Label label) {
        label.setOnMouseClicked(e -> selectedTab.set(label));
        label.setPrefWidth(120);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll(Styles.TITLE_2);


        return label;
    }

}
