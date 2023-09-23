package org.icroco.picture.views.util.widget;

import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import lombok.Getter;

public class ProgressIndicatorBar extends StackPane {
    @Getter
    final private ProgressBar bar  = new ProgressBar(0);
    final private Text        text = new Text();
    final private String      labelFormatSpecifier;

    final private static int DEFAULT_LABEL_PADDING = 5;

    public ProgressIndicatorBar(final String labelFormatSpecifier) {
        this.labelFormatSpecifier = labelFormatSpecifier;

        bar.progressProperty().addListener((observable, oldValue, newValue) -> text.setText(newValue.toString()));
        //workDone.addListener((observableValue, number, number2) -> syncProgress());

        bar.setMaxWidth(200); // allows the progress bar to expand to fill available horizontal space.
        bar.setPrefWidth(100); // allows the progress bar to expand to fill available horizontal space.

        getChildren().setAll(bar, text);
        bar.setMinHeight(text.getBoundsInLocal().getHeight() + DEFAULT_LABEL_PADDING * 2);
        bar.setMinWidth(text.getBoundsInLocal().getWidth() + DEFAULT_LABEL_PADDING * 2);
    }

    void progressChange(Number number) {

    }

    // synchronizes the progress indicated with the work done.
//    private void syncProgress() {
//        if (workDone == null || totalWork == 0) {
//            text.setText("");
//            bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
//        } else {
//            text.setText(String.format(labelFormatSpecifier, Math.ceil(workDone.get())));
//            bar.setProgress(workDone.get() / totalWork);
//        }
//
//
//    }
}