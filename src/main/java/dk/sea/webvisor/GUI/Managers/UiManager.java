package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.GUI.Controllers.MetadataDialogController;
import dk.sea.webvisor.GUI.Controllers.SlideViewController;

// Java Imports
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

public class UiManager {
    public record UiState(
            boolean running,
            boolean hasPage,
            boolean hasSelectedBox,
            boolean hasSelectedItem,
            boolean hasDocuments,
            int currentIndex,
            int totalPages
    ) {}

    private final Label statusLabel;

    public UiManager(Label statusLabel) {
        this.statusLabel = statusLabel;
    }

    public void info(String msg) {
        setStatus(msg, "status-info");
    }

    public void success(String msg) {
        setStatus(msg, "status-success");
    }

    public void error(String msg) {
        setStatus(msg, "status-error");
    }

    private void setStatus(String msg, String style) {
        Platform.runLater(() -> {
            statusLabel.getStyleClass().removeAll("status-info", "status-success", "status-error");
            statusLabel.getStyleClass().add(style);
            statusLabel.setText(msg);
        });
    }

    public void applyState(
            UiState state,
            Button btnPrev,
            Button btnNext,
            Button btnRotateLeft,
            Button btnRotateRight,
            Button btnSplit,
            Button btnStart,
            Button btnStop,
            Button btnDelete,
            Button btnBack,
            Button btnExportSingle,
            Button btnExportPdf
    ) {
        boolean hasPage = state.hasPage();
        boolean hasSelectedBox = state.hasSelectedBox();
        boolean hasSelectedItem = state.hasSelectedItem();
        boolean hasDocuments = state.hasDocuments();
        int currentIndex = state.currentIndex();
        int totalPages = state.totalPages();
        boolean running = state.running();

        btnPrev.setDisable(!hasPage);
        btnNext.setDisable(!hasPage);
        btnRotateLeft.setDisable(!hasPage);
        btnRotateRight.setDisable(!hasPage);
        btnSplit.setDisable(!hasPage || currentIndex >= totalPages - 1);

        btnStart.setDisable(running || !hasSelectedBox);
        btnStop.setDisable(!running);
        btnDelete.setDisable(running || !hasSelectedItem);
        btnBack.setDisable(!hasSelectedItem);
        btnExportSingle.setDisable(!hasSelectedBox || !hasDocuments);
        btnExportPdf.setDisable(!hasSelectedBox || !hasDocuments);
    }

    public void openMetadataDialog(String boxId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MetadataDialogView.fxml"));
            Parent root = loader.load();
            MetadataDialogController controller = loader.getController();
            controller.setBox(boxId);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Metadata");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Could not open metadata dialog: " + e.getMessage());
        }
    }

    public void openSlideView(List<Files> pages, int startIndex) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/SlideView.fxml"));
            Parent root = loader.load();
            SlideViewController controller = loader.getController();
            controller.setPages(pages, Math.max(0, startIndex));
            Stage stage = new Stage();
            stage.setTitle("Slide View");
            stage.initModality(Modality.NONE);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Could not open slide view: " + e.getMessage());
        }
    }

    public void updateScanningSummary(Label lblTotalScans, Label lblCurrentBox, int totalScans, Boxes selectedBox) {
        lblTotalScans.setText("Total Scans: " + totalScans);
        lblCurrentBox.setText(selectedBox != null ? "Current box: " + selectedBox.getBoxId() : "Current box: none");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

