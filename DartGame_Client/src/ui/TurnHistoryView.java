package ui;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Shared presentation for live and saved throws. */
public final class TurnHistoryView {
    private TurnHistoryView() {}
    public static VBox cell(int number,String darts,String summary){
        Label heading=new Label("Lượt "+number);heading.getStyleClass().add("history-heading");
        Label scores=new Label(darts),total=new Label(summary);scores.setWrapText(true);total.setWrapText(true);
        VBox box=new VBox(4,heading,scores,total);box.getStyleClass().add("history-row");box.setMinWidth(0);box.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(box,Priority.ALWAYS);return box;
    }
}
