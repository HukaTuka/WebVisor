package dk.sea.webvisor.GUI.Managers;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class ShortcutManager {
    private final Parent scopeRoot;
    private final Runnable onStart;
    private final Runnable onStop;
    private final Runnable onPrev;
    private final Runnable onNext;
    private final Runnable onRotateLeft;
    private final Runnable onRotateRight;
    private final Runnable onSplit;
    private final Runnable onDelete;
    private final Runnable onMetadata;
    private final Runnable onSlideView;
    private final Runnable onSettings;

    public ShortcutManager(
            Parent scopeRoot,
            Runnable onStart,
            Runnable onStop,
            Runnable onPrev,
            Runnable onNext,
            Runnable onRotateLeft,
            Runnable onRotateRight,
            Runnable onSplit,
            Runnable onDelete,
            Runnable onMetadata,
            Runnable onSlideView,
            Runnable onSettings
    ) {
        this.scopeRoot = scopeRoot;
        this.onStart = onStart;
        this.onStop = onStop;
        this.onPrev = onPrev;
        this.onNext = onNext;
        this.onRotateLeft = onRotateLeft;
        this.onRotateRight = onRotateRight;
        this.onSplit = onSplit;
        this.onDelete = onDelete;
        this.onMetadata = onMetadata;
        this.onSlideView = onSlideView;
        this.onSettings = onSettings;
    }

    public void install(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handle);
    }

    private void handle(KeyEvent event) {
        if (!isInsideScope(event) || isTypingContext(event)) {
            return;
        }

        KeyCode code = event.getCode();

        if (code == KeyCode.F1) {
            onStart.run();
            event.consume();
            return;
        }
        if (code == KeyCode.F2) {
            onStop.run();
            event.consume();
            return;
        }
        if (code == KeyCode.A) {
            onPrev.run();
            event.consume();
            return;
        }
        if (code == KeyCode.D) {
            onNext.run();
            event.consume();
            return;
        }
        if (code == KeyCode.Q) {
            onRotateLeft.run();
            event.consume();
            return;
        }
        if (code == KeyCode.E) {
            onRotateRight.run();
            event.consume();
            return;
        }
        if (code == KeyCode.M) {
            onMetadata.run();
            event.consume();
            return;
        }
        if (code == KeyCode.V) {
            onSlideView.run();
            event.consume();
            return;
        }
        if (code == KeyCode.F10) {
            onSettings.run();
            event.consume();
            return;
        }
        if (event.isControlDown() && code == KeyCode.S) {
            onSplit.run();
            event.consume();
            return;
        }
        if (event.isControlDown() && code == KeyCode.DELETE) {
            onDelete.run();
            event.consume();
        }
    }

    private boolean isInsideScope(KeyEvent event) {
        Object target = event.getTarget();
        if (!(target instanceof Node node)) {
            return false;
        }

        Node current = node;
        while (current != null) {
            if (current == scopeRoot) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean isTypingContext(KeyEvent event) {
        Object target = event.getTarget();
        if (!(target instanceof Node node)) {
            return false;
        }

        Node current = node;
        while (current != null) {
            if (current instanceof TextInputControl) {
                return true;
            }
            if (current instanceof ComboBoxBase<?>) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}

