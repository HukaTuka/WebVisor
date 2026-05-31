package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;
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
import java.sql.SQLException;
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
    private final ArchiveService archiveService;

    public UiManager(Label statusLabel, ArchiveService archiveService)
    {
        this.statusLabel = statusLabel;
        this.archiveService = archiveService;
    }

    public void info(String msg)    { setStatus(msg, "status-info");    }
    public void success(String msg) { setStatus(msg, "status-success"); }
    public void error(String msg)   { setStatus(msg, "status-error");   }

    private void setStatus(String msg, String style)
    {
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
            Button btnExportPdf)
    {
        btnPrev.setDisable(!state.hasPage());
        btnNext.setDisable(!state.hasPage());
        btnRotateLeft.setDisable(!state.hasPage());
        btnRotateRight.setDisable(!state.hasPage());
        btnSplit.setDisable(!state.hasPage() || state.currentIndex() >= state.totalPages() - 1);

        btnStart.setDisable(state.running() || !state.hasSelectedBox());
        btnStop.setDisable(!state.running());
        btnDelete.setDisable(state.running() || !state.hasSelectedItem());
        btnBack.setDisable(!state.hasSelectedItem());
        btnExportSingle.setDisable(!state.hasSelectedBox() || !state.hasDocuments());
        btnExportPdf.setDisable(!state.hasSelectedBox() || !state.hasDocuments());
    }

    public void openMetadataDialog(String boxId)
    {
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
            showError("Could not open metadata dialog");
        }
    }

    public void openSlideView(List<Files> pages, int startIndex)
    {
        setStatus("Loading images for slide view...", "status-info");

        Thread t = new Thread(() ->
        {
            try
            {
                for (Files page : pages)
                {
                    if (page.getImage() == null && page.getId() > 0)
                    {
                        page.setImage(archiveService.loadFileImage(page.getId()));
                    }
                }

                Platform.runLater(() ->
                {
                    try
                    {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/SlideView.fxml"));
                        Parent root = loader.load();
                        SlideViewController controller = loader.getController();
                        controller.setPages(pages, Math.max(0, startIndex));
                        Stage stage = new Stage();
                        stage.setTitle("Slide View");
                        stage.initModality(Modality.NONE);
                        stage.setScene(new Scene(root));
                        stage.show();
                        setStatus("", "status-info");
                    }
                    catch (IOException e)
                    {
                        showError("Could not open slide view");
                    }
                });
            }
            catch (SQLException e)
            {
                Platform.runLater(() -> showError("Could not load images"));
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public void openShortcutSettingsDialog()
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ShortcutSettingsView.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Shortcut Settings");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Could not open shortcut settings");
        }
    }

    public void updateScanningSummary(Label lblTotalScans, Label lblCurrentBox, int totalScans, Boxes selectedBox)
    {
        lblTotalScans.setText("Total Scans: " + totalScans);
        lblCurrentBox.setText(selectedBox != null
                ? "Current box: " + selectedBox.getBoxId()
                : "Current box: none");
    }

    private void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}