package ui;

import javafx.scene.control.Dialog;

/** Presentation helpers shared by menus and native JavaFX dialogs. */
public final class GameTheme {
    private GameTheme() {}
    public static void style(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(GameTheme.class.getResource("/views/theme.css").toExternalForm());
    }
}
