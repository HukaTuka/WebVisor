package dk.sea.webvisor.GUI.Managers;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

//code originally made by rekast
public class ComboBoxKeyboardManager {
    public void configure(ComboBox<?> combo, Control nextControl){
        if (combo == null) return;

        combo.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DOWN) {
                if (!combo.isShowing()) {
                    combo.show();
                    if(combo.getSelectionModel().getSelectedIndex() < 0 && !combo.getItems().isEmpty()) {
                        combo.getSelectionModel().selectFirst();
                    }
                    event.consume();
                    return;
                }

                if (combo.getSelectionModel().getSelectedIndex() < 0 && combo.getItems().isEmpty()) {
                    combo.getSelectionModel().selectFirst();
                    event.consume();
                    return;
                }
            }
            if(event.getCode() == KeyCode.ENTER){
                combo.hide();
                if (nextControl != null) {
                    Platform.runLater(nextControl::requestFocus);
                }
                event.consume();
            }
        });
    }
}
