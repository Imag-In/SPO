package org.icroco.picture.views.organize.gallery;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.MultiplePredicates;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GalleryFilterView implements FxView<VBox> {

    private final VBox root = new VBox();

    private final ToggleButton toggleKeep      = new ToggleButton("", new FontIcon(Material2OutlinedMZ.THUMB_UP));
    private final ToggleButton toggleThrow     = new ToggleButton("", new FontIcon(Material2OutlinedMZ.THUMB_DOWN));
    private final ToggleButton toggleUndecided = new ToggleButton("", new FontIcon(MaterialDesignH.HEAD_QUESTION_OUTLINE));
    private final ToggleGroup  toggleGroup     = new ToggleGroup();

    final FontIcon filterRemove = new FontIcon(MaterialDesignF.FILTER_VARIANT_REMOVE);
    final FontIcon filterAdd    = new FontIcon(MaterialDesignF.FILTER_VARIANT_PLUS);

    private final Button reset = new Button(null, new FontIcon(MaterialDesignD.DELETE_OUTLINE));
    private final Button ok    = new Button(null, new FontIcon(MaterialDesignC.CHECK));

    public GalleryFilterView() {
        root.setSpacing(10);
        root.setPadding(new Insets(16));

        var tileKoT = new Tile("Keep or Throw", "Filter base on Keep or Throw status");

        toggleKeep.getStyleClass().add(Styles.LEFT_PILL);
        toggleThrow.getStyleClass().add(Styles.RIGHT_PILL);
        toggleUndecided.getStyleClass().add(Styles.CENTER_PILL);
        toggleKeep.setUserData(EKeepOrThrow.KEEP);
        toggleThrow.setUserData(EKeepOrThrow.THROW);
        toggleUndecided.setUserData(EKeepOrThrow.UNKNOW);
        var hBox = new HBox(toggleKeep, toggleUndecided, toggleThrow);
        toggleGroup.getToggles().addAll(toggleKeep, toggleThrow, toggleUndecided);
        tileKoT.setAction(hBox);
        tileKoT.setActionHandler(() -> log.info("Click KoT"));
        tileKoT.setPrefWidth(400);
        root.setSpacing(10);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: -color-bg-default;");
        root.setMaxWidth(0);
        root.setMaxHeight(0);
        root.setMinWidth(500);

        HBox hb = new HBox(reset, ok);
        hb.setAlignment(Pos.CENTER_RIGHT);
        hb.setSpacing(10);
        HBox.setHgrow(hb, Priority.ALWAYS);
        reset.setOnMouseClicked(_ -> toggleGroup.selectToggle(null));
        reset.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        ok.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);

        root.getChildren().addAll(tileKoT, hb);
    }

    public void showFilter(ModalPane modalPane, Label lbFilter, MultiplePredicates<MediaFile> predicates) {
        final var subscription = toggleGroup.selectedToggleProperty().subscribe((oldValue, newValue) -> {
            if (oldValue != null) {
                predicates.remove(KoTPredicate.class);
            }
            if (newValue != null) {
                predicates.add(new KoTPredicate((EKeepOrThrow) newValue.getUserData()));
            }
        });
        ok.setOnAction(_ -> modalPane.hide());
        modalPane.displayProperty().subscribe((_, newValue) -> {
            log.info("Display visible: {}", newValue);
            if (Boolean.FALSE.equals(newValue)) {
                subscription.unsubscribe();
                ok.setOnAction(null);
                if (predicates.size() > 1) {
                    lbFilter.setGraphic(filterRemove);
                    lbFilter.getStyleClass().add(Styles.DANGER);

                } else {
                    lbFilter.setGraphic(filterAdd);
                    lbFilter.getStyleClass().remove(Styles.DANGER);
                }
            }
        });
        modalPane.show(root);
    }

    @Override
    public VBox getRootContent() {
        return root;
    }
}
