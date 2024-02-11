package org.icroco.picture.views.util;

import atlantafx.base.controls.Card;
import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;

import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElse;

@Slf4j
public class MpDialog extends Card {

    public enum EXIT_MODE {
        OK,
        CANCEL,
        RESET
    }

    private final Button ok     = new Button(null, new FontIcon(MaterialDesignC.CHECK));
    private final Button reset  = new Button(null, new FontIcon(MaterialDesignD.DELETE_OUTLINE));
    private final Button cancel = new Button(null, new FontIcon(MaterialDesignC.CANCEL));

    @Builder
    public MpDialog(Node header, Node subHeader, Node body, Integer width, Integer height, Boolean showCancel, Boolean showReset) {
        super();

        if (header != null) {
            setHeader(header);
            header.getStyleClass().add(Styles.TITLE_4);
        }
        if (subHeader != null) {
            setSubHeader(subHeader);
        }
        if (body != null) {
            setBody(body);
        }


//        setSpacing(10);
//        setAlignment(Pos.CENTER);
        setMinSize(requireNonNullElse(width, 200), requireNonNullElse(height, 200));
        setMaxSize(requireNonNullElse(width, 200), requireNonNullElse(height, 200));
        setStyle("-fx-background-color: -color-bg-default;");

        reset.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_OUTLINED, Styles.ACCENT);
        cancel.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_OUTLINED, Styles.WARNING);
        ok.getStyleClass().addAll(atlantafx.base.theme.Styles.BUTTON_OUTLINED, Styles.SUCCESS);

        showReset = requireNonNullElse(showReset, false);
        showCancel = requireNonNullElse(showCancel, false);
        if (!showReset) {
            reset.setManaged(false);
            reset.setVisible(false);
        }
        if (!showCancel) {
            cancel.setManaged(false);
            cancel.setVisible(false);
        }
    }

    public void show(ModalPane modalPane, Consumer<EXIT_MODE> consumer) {
        HBox hb = new HBox(reset, cancel, ok);
        hb.setAlignment(Pos.CENTER_RIGHT);
        hb.setSpacing(10);
        HBox.setHgrow(hb, Priority.ALWAYS);
        final var escProperty = new SimpleBooleanProperty(true);
        setFooter(hb);

        ok.setOnAction(_ -> modalPane.hide());
        modalPane.displayProperty().subscribe((_, newValue) -> {
            if (!newValue) {
                if (escProperty.get()) {
//                    log.info("Display visible: {}, exc: {}", newValue, escProperty.get());
                    consumer.accept(EXIT_MODE.CANCEL);
                }
            }
        });

        this.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                event.consume();
                okAction(escProperty, modalPane, consumer, EXIT_MODE.OK);
            }
        });
        ok.setOnAction(_ -> okAction(escProperty, modalPane, consumer, EXIT_MODE.OK));
        cancel.setOnAction(_ -> okAction(escProperty, modalPane, consumer, EXIT_MODE.CANCEL));
        reset.setOnAction(_ -> consumer.accept(EXIT_MODE.RESET));
        modalPane.show(this);
        this.requestFocus();
    }

    private static void okAction(SimpleBooleanProperty escProperty, ModalPane modalPane, Consumer<EXIT_MODE> consumer, EXIT_MODE ok) {
        escProperty.set(false);
//        log.info("esc: {}", escProperty.get());
//        Platform.runLater(() -> {
        modalPane.hide(true);
        consumer.accept(ok);
//        });
    }
}
