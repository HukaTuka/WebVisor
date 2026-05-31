package dk.sea.webvisor.GUI.Controllers;

import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.DocumentStatus;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.DAL.Interface.AuditAware;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QAController implements AuditAware
{
    @FXML private ListView<Document> lstDocuments;
    @FXML private ImageView imgPage;
    @FXML private Label lblPageInfo;
    @FXML private Label lblStatus;
    @FXML private Label lblEmpty;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Button btnInProgress;
    @FXML private Button btnWaitingQA;
    @FXML private Button btnQACompleted;
    @FXML private Button btnRejected;
    @FXML private VBox rejectionPane;
    @FXML private TextArea txtRejectionNote;

    private ArchiveService archiveService;
    private AuditService audit;

    private final ObservableList<Document> documents = FXCollections.observableArrayList();
    private Document selectedDocument;
    private final List<Files> currentPages = new ArrayList<>();
    private int pageIndex = -1;

    @Override
    public void setAudit(AuditService audit)
    {
        this.audit = audit;

        try
        {
            archiveService = new ArchiveService();
        }
        catch (IOException e)
        {
            showStatus("Could not initialise archive service.", "status-error");
            return;
        }

        refreshDocuments();
    }

    @FXML
    private void initialize()
    {
        setupDocumentList();
        btnInProgress.setManaged(false);
    }

    private void setupDocumentList()
    {
        lstDocuments.setItems(documents);

        lstDocuments.setCellFactory(lv -> new ListCell<>()
        {
            @Override
            protected void updateItem(Document item, boolean empty)
            {
                super.updateItem(item, empty);
                if (empty || item == null)
                {
                    setText(null);
                    return;
                }
                String box = item.getBoxId().isBlank() ? "Unknown box" : item.getBoxId();
                setText(box + "  /  Document " + item.getDocumentNumber()
                        + "  [" + item.getStatus().getDisplayName() + "]");
            }
        });

        lstDocuments.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
        {
            if (selected != null)
            {
                openDocument(selected);
            }
        });
    }

    @FXML
    private void onRefresh()
    {
        selectedDocument = null;
        currentPages.clear();
        pageIndex = -1;
        imgPage.setImage(null);
        lblPageInfo.setText("Page 0 / 0");
        lblEmpty.setVisible(true);
        lblEmpty.setManaged(true);
        rejectionPane.setVisible(false);
        rejectionPane.setManaged(false);
        refreshDocuments();
        updateControls();
    }

    private void refreshDocuments()
    {
        try
        {
            List<Document> waiting = archiveService.getDocumentsForQA();
            documents.setAll(waiting);

            if (documents.isEmpty())
            {
                showStatus("No documents are currently waiting for QA.", "status-info");
            }
            else
            {
                showStatus(documents.size() + " document(s) awaiting review.", "status-info");
            }
        }
        catch (SQLException e)
        {
            showStatus("Could not load QA documents: " + e.getMessage(), "status-error");
        }
    }

    private void openDocument(Document document)
    {
        selectedDocument = document;
        currentPages.clear();
        pageIndex = -1;
        imgPage.setImage(null);

        try
        {
            List<Files> pages = archiveService.getPagesByDocument(document.getId());
            currentPages.addAll(pages);
        }
        catch (SQLException e)
        {
            showStatus("Could not load pages: " + e.getMessage(), "status-error");
            updateControls();
            return;
        }

        boolean isRejected = document.getStatus() == DocumentStatus.REJECTED;
        rejectionPane.setVisible(isRejected);
        rejectionPane.setManaged(isRejected);
        txtRejectionNote.setText(document.getRejectionNote());

        lblEmpty.setVisible(currentPages.isEmpty());
        lblEmpty.setManaged(currentPages.isEmpty());

        if (!currentPages.isEmpty())
        {
            goToPage(0);
        }

        updateControls();
    }

    @FXML
    private void onPrev()
    {
        goToPage(pageIndex - 1);
    }

    @FXML
    private void onNext()
    {
        goToPage(pageIndex + 1);
    }

    private void goToPage(int index)
    {
        if (index < 0 || index >= currentPages.size()) return;

        pageIndex = index;
        Files page = currentPages.get(pageIndex);

        try
        {
            if (page.getImage() == null && page.getId() > 0)
            {
                page.setImage(archiveService.loadFileImage(page.getId()));
            }
        }
        catch (SQLException e)
        {
            showStatus("Could not load image: " + e.getMessage(), "status-error");
        }

        if (page.getImage() != null)
        {
            BufferedImage rotated = applyRotation(page.getImage(), page.getRotationDegrees());
            imgPage.setImage(SwingFXUtils.toFXImage(rotated, null));
        }
        else
        {
            imgPage.setImage(null);
        }

        lblEmpty.setVisible(false);
        lblEmpty.setManaged(false);

        String barcodeNote = page.isBarcode() ? "  [BARCODE]" : "";
        lblPageInfo.setText("Page " + (pageIndex + 1) + " / " + currentPages.size() + barcodeNote);
        updateControls();
    }

    @FXML
    private void onMarkInProgress()
    {
        applyStatusChange(DocumentStatus.IN_PROGRESS);
        refreshDocuments();
    }

    @FXML
    private void onMarkWaitingQA()
    {
        applyStatusChange(DocumentStatus.WAITING_FOR_QA);
        refreshDocuments();
    }

    @FXML
    private void onMarkQACompleted()
    {
        applyStatusChange(DocumentStatus.QA_COMPLETED);
        refreshDocuments();
    }

    @FXML
    private void onMarkRejected()
    {
        applyStatusChange(DocumentStatus.REJECTED);
        refreshDocuments();
    }

    @FXML
    private void onSaveRejectionNote()
    {
        if (selectedDocument == null) return;

        String note = txtRejectionNote.getText() == null ? "" : txtRejectionNote.getText().trim();

        try
        {
            archiveService.updateRejectionNote(selectedDocument.getId(), note);
            selectedDocument.setRejectionNote(note);
            showStatus("Rejection note saved.", "status-success");
            audit.log("Rejection note saved",
                    "Document " + selectedDocument.getDocumentNumber()
                            + " in box " + selectedDocument.getBoxId() + ": " + note);
        }
        catch (SQLException e)
        {
            showStatus("Could not save rejection note", "status-error");
        }
    }

    private void applyStatusChange(DocumentStatus status)
    {
        if (selectedDocument == null) return;

        try
        {
            archiveService.updateDocumentStatus(selectedDocument.getId(), status);
            selectedDocument.setStatus(status);

            // Update the list cell in place without re-fetching from the database,
            // so the document stays visible regardless of which status is applied.
            int listIndex = documents.indexOf(selectedDocument);
            if (listIndex >= 0)
            {
                documents.set(listIndex, selectedDocument);
                lstDocuments.getSelectionModel().select(listIndex);
            }

            boolean isRejected = status == DocumentStatus.REJECTED;
            rejectionPane.setVisible(isRejected);
            rejectionPane.setManaged(isRejected);

            if (!isRejected)
            {
                txtRejectionNote.clear();
            }

            audit.log("QA status updated",
                    "Document " + selectedDocument.getDocumentNumber()
                            + " in box " + selectedDocument.getBoxId()
                            + " set to: " + status.getDisplayName());

            showStatus("Document marked as: " + status.getDisplayName(), "status-success");
            updateControls();
        }
        catch (SQLException e)
        {
            showStatus("Could not update document status", "status-error");
        }
    }

    private void updateControls()
    {
        boolean hasDocument = selectedDocument != null;
        boolean hasPages = !currentPages.isEmpty();

        btnPrev.setDisable(!hasPages || pageIndex <= 0);
        btnNext.setDisable(!hasPages || pageIndex >= currentPages.size() - 1);
        btnInProgress.setDisable(!hasDocument);
        btnWaitingQA.setDisable(!hasDocument);
        btnQACompleted.setDisable(!hasDocument);
        btnRejected.setDisable(!hasDocument);
    }

    private BufferedImage applyRotation(BufferedImage source, int degrees)
    {
        if (degrees == 0) return source;

        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth  = (int) Math.floor(source.getWidth() * cos + source.getHeight() * sin);
        int newHeight = (int) Math.floor(source.getWidth() * sin + source.getHeight() * cos);

        int imageType = source.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : source.getType();
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, imageType);

        AffineTransform transform = new AffineTransform();
        transform.translate(
                (newWidth - source.getWidth()) / 2.0,
                (newHeight - source.getHeight()) / 2.0);
        transform.rotate(radians, source.getWidth() / 2.0, source.getHeight() / 2.0);

        new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR).filter(source, rotated);
        return rotated;
    }

    private void showStatus(String message, String styleClass)
    {
        Platform.runLater(() ->
        {
            lblStatus.getStyleClass().removeAll("status-info", "status-success", "status-error");
            lblStatus.getStyleClass().add(styleClass);
            lblStatus.setText(message);
        });
    }
}