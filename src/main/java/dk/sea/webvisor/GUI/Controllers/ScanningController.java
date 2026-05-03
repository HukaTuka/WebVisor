package dk.sea.webvisor.GUI.Controllers;

import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.ScanBox;
import dk.sea.webvisor.BE.ScannedPage;
import dk.sea.webvisor.BLL.ScanningService;
import dk.sea.webvisor.BLL.Util.AuditService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class ScanningController
{
    private static final String BARCODE_CELL_STYLE = "barcode-item-cell";

    private enum ExplorerLevel
    {
        BOXES, DOCUMENTS, FILES
    }

    @FXML
    private TextField txtBoxId;
    @FXML
    private Label lblCurrentBox;
    @FXML
    private Label lblExplorerPath;
    @FXML
    private Button btnBack;
    @FXML
    private ListView<Object> lstExplorer;
    @FXML
    private Label lblTotalScans;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnStop;
    @FXML
    private Button btnRotateLeft;
    @FXML
    private Button btnRotateRight;
    @FXML
    private Button btnPrev;
    @FXML
    private Button btnNext;
    @FXML
    private Button btnDelete;
    @FXML
    private Label lblPageInfo;
    @FXML
    private Label lblStatus;
    @FXML
    private ImageView imgPage;

    private final ScanningService scanningService = new ScanningService();
    private final AuditService audit = AuditService.getInstance();
    private final ObservableList<ScannedPage> pageItems = FXCollections.observableArrayList();
    private final ObservableList<ScanBox> boxItems = FXCollections.observableArrayList();

    private volatile boolean running = false;
    private Thread pollingThread = null;
    private int currentIndex = -1;
    private ScanBox selectedBox = null;
    private Document selectedDocument = null;
    private ExplorerLevel currentLevel = ExplorerLevel.BOXES;

    /** Interval between API polls while a session is active (milliseconds). */
    private static final long POLL_INTERVAL_MS = 0;


    @FXML
    private void initialize()
    {
        lstExplorer.setCellFactory(lv -> new ListCell<>()
        {
            @Override
            protected void updateItem(Object item, boolean empty)
            {
                super.updateItem(item, empty);
                getStyleClass().remove(BARCODE_CELL_STYLE);

                if (empty || item == null)
                {
                    setText(null);
                    return;
                }

                if (item instanceof ScanBox box)
                {
                    setText(box.toString());
                }
                else if (item instanceof Document document)
                {
                    setText(document.toString());
                }
                else if (item instanceof ScannedPage page)
                {
                    if (page.isBarcode())
                    {
                        setText(page.getReferenceId() + " [BARCODE]");
                        if (!getStyleClass().contains(BARCODE_CELL_STYLE))
                        {
                            getStyleClass().add(BARCODE_CELL_STYLE);
                        }
                    }
                    else
                    {
                        setText(page.getReferenceId());
                    }
                }
            }
        });

        lstExplorer.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1)
            {
                openSelectedItem();
            }
        });

        showBoxesLevel();
        updateCurrentBoxLabel();
        updateTotalScansLabel();
        updatePageLabel();
        updateButtonState();
    }

    @FXML
    private void onCreateBox()
    {
        String boxId = txtBoxId.getText() == null ? "" : txtBoxId.getText().trim();
        if (boxId.isEmpty())
        {
            showStatus("Please enter a Box ID.", "status-error");
            return;
        }

        for (ScanBox box : boxItems)
        {
            if (box.getBoxId().equalsIgnoreCase(boxId))
            {
                showStatus("Box ID already exists.", "status-error");
                return;
            }
        }

        ScanBox newBox = new ScanBox(boxId);
        boxItems.add(newBox);
        txtBoxId.clear();
        showBoxesLevel();
        lstExplorer.getSelectionModel().select(newBox);
        showStatus("Created box " + boxId + ". Click it to open.", "status-success");
        audit.log("BOX_CREATED", "Created box " + boxId);
    }

    @FXML
    private void onBack()
    {
        if (currentLevel == ExplorerLevel.FILES)
        {
            showDocumentsLevel();
            return;
        }

        if (currentLevel == ExplorerLevel.DOCUMENTS)
        {
            selectedBox = null;
            selectedDocument = null;
            showBoxesLevel();
        }
    }

    @FXML
    private void onStartScanning()
    {
        if (running)
        {
            return;
        }

        if (selectedBox == null)
        {
            showStatus("Open a box before scanning.", "status-error");
            return;
        }

        if (!pageItems.isEmpty())
        {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Start a new session in this box? This will clear current files in the box.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
            {
                return;
            }
        }

        scanningService.clearSession();
        selectedBox.clearContent();
        pageItems.clear();
        selectedDocument = null;
        currentIndex = -1;
        if (imgPage != null)
        {
            imgPage.setImage(null);
        }
        updateTotalScansLabel();
        updatePageLabel();
        showFilesLevel();
        showStatus("Scanning started — polling API every " + POLL_INTERVAL_MS / 1000 + " s…", "status-info");
        audit.log("SCAN_STARTED", "Scanning started in box " + selectedBox.getBoxId());

        running = true;
        updateButtonState();

        pollingThread = new Thread(this::pollLoop, "scanning-poll-thread");
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    @FXML
    private void onStopScanning()
    {
        running = false;
        updateButtonState();
        updateBoxSnapshotFromCurrentSession();
        showStatus("Scanning stopped. " + pageItems.size() + " file(s) in this box.", "status-info");
        audit.log("SCAN_STOPPED", "Scanning stopped in box "
                + (selectedBox == null ? "none" : selectedBox.getBoxId())
                + ". Total files: " + pageItems.size());
    }

    @FXML
    private void onPrev()
    {
        if (currentIndex > 0)
        {
            navigateTo(currentIndex - 1);
        }
    }

    @FXML
    private void onNext()
    {
        if (currentIndex < pageItems.size() - 1)
        {
            navigateTo(currentIndex + 1);
        }
    }


    @FXML
    private void onRotateLeft()
    {
        applyRotation(false);
    }

    @FXML
    private void onRotateRight()
    {
        applyRotation(true);
    }

    @FXML
    public void onDelete(ActionEvent actionEvent)
    {
        if (pageItems.isEmpty() || currentIndex < 0 || currentIndex >= pageItems.size())
        {
            showStatus("No file selected for deletion.", "status-error");
            return;
        }

        int deleteIndex = currentIndex;
        if (!scanningService.deletePageAt(deleteIndex))
        {
            showStatus("Could not delete selected file.", "status-error");
            return;
        }

        audit.log("PAGE_DELETED", "Page at index " + deleteIndex + " was deleted");
        pageItems.setAll(scanningService.getAllPages());

        if (!pageItems.isEmpty())
        {
            navigateTo(Math.min(deleteIndex, pageItems.size() - 1));
        }
        else
        {
            currentIndex = -1;
            if (imgPage != null)
            {
                imgPage.setImage(null);
            }
            updatePageLabel();
        }

        updateButtonState();
        updateTotalScansLabel();
        updateBoxSnapshotFromCurrentSession();
        if (currentLevel == ExplorerLevel.FILES)
        {
            showFilesLevel();
        }
    }

    private void pollLoop()
    {
        while (running)
        {
            try
            {
                List<ScannedPage> newPages = scanningService.fetchAndAppendNext();

                Platform.runLater(() -> handleNewPages(newPages));

                Thread.sleep(POLL_INTERVAL_MS);
            }
            catch (IOException e)
            {
                Platform.runLater(() ->
                        showStatus("API error: " + e.getMessage() + " — retrying…", "status-error"));

                try { Thread.sleep(POLL_INTERVAL_MS); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleNewPages(List<ScannedPage> newPages)
    {
        if (newPages.isEmpty())
        {
            return;
        }

        pageItems.addAll(newPages);
        navigateTo(pageItems.size() - 1);
        updateTotalScansLabel();
        updateBoxSnapshotFromCurrentSession();

        audit.log("NEW_PAGES", "Page at index " + pageItems.size() + " was created");

        if (currentLevel == ExplorerLevel.FILES)
        {
            showFilesLevel();
        }

        boolean barcodeFound = newPages.stream().anyMatch(ScannedPage::isBarcode);
        if (barcodeFound)
        {
            int splitPage = newPages.stream()
                    .filter(ScannedPage::isBarcode)
                    .mapToInt(ScannedPage::getPageNumber)
                    .min()
                    .orElse(-1);

            //Audit: barcode / document split detected
            audit.log("BARCODE_DETECTED", "Document split barcode detected at page " + splitPage);

            showStatus("⚠ Barcode detected — document split at page " +
                    newPages.stream().filter(dk.sea.webvisor.BE.ScannedPage::isBarcode)
                            .mapToInt(ScannedPage::getPageNumber).min().orElse(-1),
                    "status-error");
        }
        else
        {
            showStatus("Page " + newPages.get(0).getPageNumber() + " received.", "status-info");
        }
    }

    private void openSelectedItem()
    {
        if (running && currentLevel != ExplorerLevel.FILES)
        {
            showStatus("Stop scanning before changing box/document.", "status-error");
            return;
        }

        Object selected = lstExplorer.getSelectionModel().getSelectedItem();
        if (selected == null)
        {
            return;
        }

        if (currentLevel == ExplorerLevel.BOXES && selected instanceof ScanBox box)
        {
            selectedBox = box;
            selectedDocument = null;
            pageItems.setAll(box.getPages());
            if (pageItems.isEmpty())
            {
                currentIndex = -1;
                if (imgPage != null)
                {
                    imgPage.setImage(null);
                }
                updatePageLabel();
            }
            else
            {
                navigateTo(pageItems.size() - 1);
            }
            updateCurrentBoxLabel();
            updateTotalScansLabel();
            showDocumentsLevel();
            showStatus("Opened box " + box.getBoxId(), "status-info");
            audit.log("BOX_OPENED", "Opened box " + box.getBoxId());
            return;
        }

        if (currentLevel == ExplorerLevel.DOCUMENTS && selected instanceof Document document)
        {
            selectedDocument = document;
            showFilesLevel();
            showStatus("Opened " + document, "status-info");
            return;
        }

        if (currentLevel == ExplorerLevel.FILES && selected instanceof ScannedPage page)
        {
            int index = pageItems.indexOf(page);
            if (index >= 0)
            {
                navigateTo(index);
            }
        }
    }

    private void showBoxesLevel()
    {
        currentLevel = ExplorerLevel.BOXES;
        lstExplorer.setItems(FXCollections.observableArrayList(boxItems));
        lblExplorerPath.setText("Boxes");
        btnBack.setDisable(true);
        updateButtonState();
    }

    private void showDocumentsLevel()
    {
        currentLevel = ExplorerLevel.DOCUMENTS;
        if (selectedBox == null)
        {
            lstExplorer.setItems(FXCollections.observableArrayList());
            lblExplorerPath.setText("Boxes");
            btnBack.setDisable(true);
            return;
        }

        lstExplorer.setItems(FXCollections.observableArrayList(selectedBox.getDocuments()));
        lblExplorerPath.setText("Boxes / " + selectedBox.getBoxId() + " / Documents");
        btnBack.setDisable(false);
        updateButtonState();
    }

    private void showFilesLevel()
    {
        currentLevel = ExplorerLevel.FILES;
        if (selectedDocument == null)
        {
            lstExplorer.setItems(FXCollections.observableArrayList(pageItems));
            if (selectedBox == null)
            {
                lblExplorerPath.setText("Files");
            }
            else
            {
                lblExplorerPath.setText("Boxes / " + selectedBox.getBoxId() + " / Files");
            }
            btnBack.setDisable(false);
            updateButtonState();
            return;
        }

        lstExplorer.setItems(FXCollections.observableArrayList(selectedDocument.getPages()));
        lblExplorerPath.setText("Boxes / " + selectedBox.getBoxId() + " / " + selectedDocument);
        btnBack.setDisable(false);
        updateButtonState();
    }

    private void navigateTo(int index)
    {
        if (index < 0 || index >= pageItems.size())
        {
            return;
        }

        currentIndex = index;
        displayPage(pageItems.get(index));
        updatePageLabel();
        updateNavigationButtons();
    }

    private void displayPage(ScannedPage page) {
        if (imgPage == null) {
            return;
        }

        BufferedImage raw = page.getImage();
        BufferedImage rotated = applyAwtRotation(raw, page.getRotationDegrees());
        Image         fxImage = SwingFXUtils.toFXImage(rotated, null);

        imgPage.setImage(fxImage);
        imgPage.setOpacity(page.isBarcode() ? 0.55 : 1.0);
        imgPage.setRotate(0);
    }

    private BufferedImage applyAwtRotation(BufferedImage src, int degrees)
    {
        if (degrees == 0)
        {
            return src;
        }

        double radians = Math.toRadians(degrees);
        double sin     = Math.abs(Math.sin(radians));
        double cos     = Math.abs(Math.cos(radians));

        int newW = (int) Math.floor(src.getWidth() * cos + src.getHeight() * sin);
        int newH = (int) Math.floor(src.getWidth() * sin + src.getHeight() * cos);

        BufferedImage rotated = new BufferedImage(newW, newH, src.getType() == 0
                ? BufferedImage.TYPE_INT_ARGB
                : src.getType());

        AffineTransform tx = new AffineTransform();
        tx.translate((newW - src.getWidth()) / 2.0, (newH - src.getHeight()) / 2.0);
        tx.rotate(radians, src.getWidth() / 2.0, src.getHeight() / 2.0);

        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        op.filter(src, rotated);

        return rotated;
    }

    private void applyRotation(boolean clockwise)
    {
        if (currentIndex < 0 || currentIndex >= pageItems.size())
        {
            return;
        }

        ScannedPage page = pageItems.get(currentIndex);
        if (clockwise)
        {
            page.rotateRight();
        }
        else
        {
            page.rotateLeft();
        }

        displayPage(page);
    }

    private void updatePageLabel()
    {
        int total = pageItems.size();
        int shown = total == 0 ? 0 : currentIndex + 1;
        lblPageInfo.setText("Page " + shown + " / " + total);
    }

    private void updateNavigationButtons()
    {
        btnPrev.setDisable(currentIndex <= 0);
        btnNext.setDisable(currentIndex >= pageItems.size() - 1 || pageItems.isEmpty());
    }

    private void updateButtonState()
    {
        btnStart.setDisable(running || selectedBox == null);
        btnStop.setDisable(!running);

        boolean hasPage = currentIndex >= 0 && currentIndex < pageItems.size();
        btnRotateLeft.setDisable(!hasPage);
        btnRotateRight.setDisable(!hasPage);
        btnDelete.setDisable(!hasPage);
        updateNavigationButtons();
    }

    private void updateTotalScansLabel()
    {
        lblTotalScans.setText("Total Scans: " + pageItems.size());
    }

    private void updateCurrentBoxLabel()
    {
        lblCurrentBox.setText(selectedBox == null ? "Current box: none" : "Current box: " + selectedBox.getBoxId());
    }

    private void updateBoxSnapshotFromCurrentSession()
    {
        if (selectedBox == null)
        {
            return;
        }

        selectedBox.replaceContent(scanningService.getAllPages(), scanningService.getDocuments());
        if (currentLevel == ExplorerLevel.DOCUMENTS)
        {
            showDocumentsLevel();
        }
        if (currentLevel == ExplorerLevel.FILES)
        {
            if (selectedDocument != null)
            {
                Document foundDocument = null;
                for (Document document : selectedBox.getDocuments())
                {
                    if (document.getDocumentNumber() == selectedDocument.getDocumentNumber())
                    {
                        foundDocument = document;
                        break;
                    }
                }
                selectedDocument = foundDocument;
            }
            showFilesLevel();
        }
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }
}
