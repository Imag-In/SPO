package org.icroco.picture.views.organize.gallery;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.TagsField;
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
import org.icroco.picture.metadata.IKeywordManager;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.ERating;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.converter.KeywordStringConverter;
import org.icroco.picture.views.organize.gallery.predicate.KeywordsPredicate;
import org.icroco.picture.views.organize.gallery.predicate.KoTPredicate;
import org.icroco.picture.views.organize.gallery.predicate.RatingPredicate;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.MultiplePredicates;
import org.icroco.picture.views.util.Rating;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GalleryFilterView implements FxView<VBox> {
    private final IKeywordManager keywordManager;

    private final VBox root = new VBox();

    private final ToggleGroup        toggleGroup  = new ToggleGroup();
    private final Rating             rating       = new Rating();
    private final TagsField<Keyword> keywords     = new TagsField<>();
    final         FontIcon           filterRemove = new FontIcon(MaterialDesignF.FILTER_VARIANT_REMOVE);
    final         FontIcon           filterAdd    = new FontIcon(MaterialDesignF.FILTER_VARIANT_PLUS);

    private final Button        reset       = new Button(null, new FontIcon(MaterialDesignD.DELETE_OUTLINE));
    private final Button        ok          = new Button(null, new FontIcon(MaterialDesignC.CHECK));
    private final List<Keyword> allKeywords = new ArrayList<>();


    public GalleryFilterView(IKeywordManager keywordManager) {
        this.keywordManager = keywordManager;

        keywords.setSuggestionProvider(request -> allKeywords.stream()
                                                             .filter(kw -> kw.name()
                                                                             .toLowerCase()
                                                                             .contains(request.getUserText().toLowerCase()))
                                                             .collect(Collectors.toList()));
        keywords.setNewItemProducer(GalleryFilterView::keywordProducer);
        keywords.setConverter(new KeywordStringConverter());
        keywords.setMatcher((kw, searchText) -> kw.name().toLowerCase().startsWith(searchText.toLowerCase()));
        keywords.setComparator(Comparator.comparing(Keyword::name));

        root.setSpacing(10);
        root.setPadding(new Insets(16));

        var tileKoT     = createKeepOrThrowFilter();
        var tileRating  = createRatingFilter();
        var tileKeyword = createKeywordFilter();

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

        reset.setOnMouseClicked(_ -> resetFilter());
        reset.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        ok.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);

        root.getChildren().addAll(tileKoT, tileRating, tileKeyword, hb);
    }

    private void resetFilter() {
        toggleGroup.selectToggle(null);
        keywords.clearTags();
        rating.setValue(ERating.ABSENT);
    }


    private static Keyword keywordProducer(String name) {
        return Keyword.builder().id(null).name(name).build();
    }
    private Tile createKeepOrThrowFilter() {
        var tileKoT = new Tile("Keep or Throw", "Filter base on Keep or Throw status"); // I18N:

        var toggleKeep      = new ToggleButton("", new FontIcon(Material2OutlinedMZ.THUMB_UP));
        var toggleThrow     = new ToggleButton("", new FontIcon(Material2OutlinedMZ.THUMB_DOWN));
        var toggleUndecided = new ToggleButton("", new FontIcon(MaterialDesignH.HEAD_QUESTION_OUTLINE));

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

        return tileKoT;
    }

    private Tile createRatingFilter() {
        var tileRating = new Tile("Rating", "Filter based on rating"); // I18N:

        rating.setIconSize(24);
        tileRating.setAction(rating);
        tileRating.setActionHandler(() -> log.info("Click KoT"));
        tileRating.setPrefWidth(400);

        return tileRating;
    }

    private Tile createKeywordFilter() {
        var tileRating = new Tile("Keyword", "Filter based on keyword (tags)"); // I18N:

        tileRating.setAction(keywords);
        tileRating.setActionHandler(() -> log.info("Click KoT"));
        tileRating.setPrefWidth(400);

        return tileRating;
    }

    public void showFilter(ModalPane modalPane, Label lbFilter, MultiplePredicates<MediaFile> predicates) {
        allKeywords.addAll(keywordManager.getAll());
        final var kotSubscription = toggleGroup.selectedToggleProperty().subscribe((oldValue, newValue) -> {
            if (oldValue != null) {
                predicates.remove(KoTPredicate.class);
            }
            if (newValue != null) {
                predicates.add(new KoTPredicate((EKeepOrThrow) newValue.getUserData()));
            }
        });
        final var ratingSubscription = rating.getRatingProperty().subscribe((oldValue, newValue) -> {
            if (oldValue != null) {
                predicates.remove(RatingPredicate.class);
            }
            if (newValue != null && newValue != ERating.ABSENT) {
                predicates.add(new RatingPredicate(newValue));
            }
        });
        final var kwSubscription = keywords.tagsProperty().subscribe((oldValue, newValue) -> {
            if (oldValue != null) {
                predicates.remove(KeywordsPredicate.class);
            }
            if (newValue != null && !newValue.isEmpty()) {
                predicates.add(new KeywordsPredicate(newValue));
            }
        });

        ok.setOnAction(_ -> modalPane.hide());
        modalPane.displayProperty().subscribe((_, newValue) -> {
            log.info("Display visible: {}", newValue);
            if (Boolean.FALSE.equals(newValue)) {
                kotSubscription.unsubscribe();
                ratingSubscription.unsubscribe();
                kwSubscription.unsubscribe();
                allKeywords.clear();
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
