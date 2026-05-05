package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.ScanningService;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.BLL.ExportService;
import dk.sea.webvisor.BLL.ProfileService;

// Java Imports
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.control.ComboBox;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.util.ArrayList;

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
    private Button btnSlideView;
    @FXML
    private Label lblPageInfo;
    @FXML
    private Label lblStatus;
    @FXML
    private ImageView imgPage;
    @FXML private ComboBox<Profile> cmbProfile;
    @FXML private Button btnExportSingle;
    @FXML private Button btnExportMulti;

    private final ScanningService scanningService = new ScanningService();
    private final AuditService audit = AuditService.getInstance();
    private final ObservableList<Files> pageItems = FXCollections.observableArrayList();
    private final ObservableList<Boxes> boxItems = FXCollections.observableArrayList();
    private final Set<String> loadedBoxContent = new HashSet<>();
    private ArchiveService archiveService;
    private final ExportService exportService = new ExportService();
    private ProfileService profileService;

    private volatile boolean running = false;
    private Thread pollingThread = null;
    private int currentIndex = -1;
    private Boxes selectedBox = null;
    private Document selectedDocument = null;
    private ExplorerLevel currentLevel = ExplorerLevel.BOXES;

    /** Interval between API polls while a session is active (milliseconds). */
    private static final long POLL_INTERVAL_MS = 0;


    @FXML
    private void initialize()
    {
        try
        {
            archiveService = new ArchiveService();
            boxItems.setAll(archiveService.getAllBoxes());
        }
        catch (IOException | SQLException e)
        {
            showStatus("Could not load archive from database: " + e.getMessage(), "status-error");
        }
        try
        {
            profileService = new ProfileService();
            cmbProfile.setItems(FXCollections.observableArrayList(profileService.getAllProfiles()));
        }
        catch (IOException | SQLException e)
        {
            showStatus("Could not load profiles: " + e.getMessage(), "status-error");
        }

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

                if (item instanceof Boxes box)
                {
                    setText(box.toString());
                }
                else if (item instanceof Document document)
                {
                    setText(document.toString());
                }
                else if (item instanceof Files page)
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

        for (Boxes box : boxItems)
        {
            if (box.getBoxId().equalsIgnoreCase(boxId))
            {
                showStatus("Box ID already exists.", "status-error");
                return;
            }
        }

        if (archiveService != null)
        {
            try
            {
                archiveService.createBox(boxId);
            }
            catch (SQLException e)
            {
                showStatus("Could not create box in database: " + e.getMessage(), "status-error");
                return;
            }
        }

        Boxes newBox = new Boxes(boxId);
        boxItems.add(newBox);
        loadedBoxContent.add(boxId);
        txtBoxId.clear();
        showBoxesLevel();
        lstExplorer.getSelectionModel().select(newBox);
        showStatus("Created box " + boxId + ". Click it to open.", "status-success");
        audit.log("BOX_CREATED", "Created box " + boxId);
    }

    @FXML
    private void onExportSinglePage()
    {
        performExport(true);
    }

    @FXML
    private void onExportMultiPage()
    {
        performExport(false);
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

        if (archiveService != null && selectedBox != null)
        {
            try
            {
                archiveService.saveBoxSnapshot(selectedBox);
            }
            catch (SQLException e)
            {
                showStatus("Could not save box to database: " + e.getMessage(), "status-error");
                return;
            }
        }

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
                List<Files> newPages = scanningService.fetchAndAppendNext();

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

    private void handleNewPages(List<Files> newPages)
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

        boolean barcodeFound = newPages.stream().anyMatch(Files::isBarcode);
        if (barcodeFound)
        {
            int splitPage = newPages.stream()
                    .filter(Files::isBarcode)
                    .mapToInt(Files::getPageNumber)
                    .min()
                    .orElse(-1);

            //Audit: barcode / document split detected
            audit.log("BARCODE_DETECTED", "Document split barcode detected at page " + splitPage);

            showStatus("⚠ Barcode detected — document split at page " +
                    newPages.stream().filter(Files::isBarcode)
                            .mapToInt(Files::getPageNumber).min().orElse(-1),
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

        if (currentLevel == ExplorerLevel.BOXES && selected instanceof Boxes box)
        {
            if (archiveService != null && !loadedBoxContent.contains(box.getBoxId()))
            {
                try
                {
                    Boxes hydrated = archiveService.loadBoxContent(box.getBoxId());
                    box.replaceContent(hydrated.getPages(), hydrated.getDocuments());
                    loadedBoxContent.add(box.getBoxId());
                }
                catch (SQLException e)
                {
                    showStatus("Could not load box content: " + e.getMessage(), "status-error");
                    return;
                }
            }

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

        if (currentLevel == ExplorerLevel.FILES && selected instanceof Files page)
        {
            int index = findPageIndex(page);
            if (index >= 0)
            {
                navigateTo(index);
            }
        }
    }

    private int findPageIndex(Files selectedPage)
    {
        if (selectedPage.getId() > 0)
        {
            for (int i = 0; i < pageItems.size(); i++)
            {
                if (pageItems.get(i).getId() == selectedPage.getId())
                {
                    return i;
                }
            }
        }

        return pageItems.indexOf(selectedPage);
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

    private void displayPage(Files page) {
        if (imgPage == null) {
            return;
        }

        if (page.getImage() == null)
        {
            if (archiveService == null || page.getId() <= 0)
            {
                showStatus("Could not load image for selected file.", "status-error");
                return;
            }

            try
            {
                page.setImage(archiveService.loadFileImage(page.getId()));
            }
            catch (SQLException e)
            {
                showStatus("Could not load image: " + e.getMessage(), "status-error");
                return;
            }
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

        Files page = pageItems.get(currentIndex);
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
        boolean canExport = !running
                && selectedBox != null
                && !selectedBox.getDocuments().isEmpty();
        btnExportSingle.setDisable(!canExport);
        btnExportMulti.setDisable(!canExport);
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

    @FXML
    private void onOpenSlideView()
    {
        if (pageItems.isEmpty())
        {
            showStatus("No pages to view.", "status-error");
            return;
        }

        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/SlideView.fxml"));
            Parent root = loader.load();

            SlideViewController controller = loader.getController();
            controller.setPages(List.copyOf(pageItems), Math.max(currentIndex, 0));

            Stage stage = new Stage();
            stage.setTitle("Slide View");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imgPage.getScene().getWindow());
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.show();

            audit.log("SLIDE_VIEW_OPENED", "Opened slide view at page " + (currentIndex + 1));
        }
        catch (IOException e)
        {
            e.printStackTrace(); // <-- ADD THIS
            showStatus("Could not open slide view: " + e.getMessage(), "status-error");
        }
    }

    private void performExport(boolean singlePage)
    {
        if (selectedBox == null || selectedBox.getDocuments().isEmpty())
        {
            showStatus("No documents to export. Complete and stop a scanning session first.", "status-error");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Export Output Directory");

        // Set initial directory to Downloads if available
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.exists() && downloads.isDirectory()) {
            chooser.setInitialDirectory(downloads);
        }

        File outputDirectory = chooser.showDialog(btnExportSingle.getScene().getWindow());

        if (outputDirectory == null) { return; }

        String folderName = buildExportFolderName();
        List<Document> documents = new ArrayList<>(selectedBox.getDocuments());

        btnExportSingle.setDisable(true);
        btnExportMulti.setDisable(true);
        showStatus("Exporting — please wait…", "status-info");

        String threadName = singlePage ? "export-single-page-thread" : "export-multi-page-thread";
        Thread exportThread = new Thread(() ->
        {
            try
            {
                ensureImagesLoaded(documents);

                if (singlePage)
                {
                    int count = exportService.exportSinglePage(documents, outputDirectory, folderName);
                    String message = "Single-page export complete: " + count
                            + " file(s) written to \"" + folderName + "\".";
                    Platform.runLater(() ->
                    {
                        showStatus(message, "status-success");
                        audit.log("EXPORT_SINGLE", message);
                        updateButtonState();
                    });
                }
                else
                {
                    int count = exportService.exportMultiPage(documents, outputDirectory, folderName);
                    String message = "Multi-page export complete: " + count
                            + " document(s) written to \"" + folderName + "\".";
                    Platform.runLater(() ->
                    {
                        showStatus(message, "status-success");
                        audit.log("EXPORT_MULTI", message);
                        updateButtonState();
                    });
                }
            }
            catch (SQLException e)
            {
                Platform.runLater(() ->
                {
                    showStatus("Could not load image data for export: " + e.getMessage(), "status-error");
                    updateButtonState();
                });
            }
            catch (IOException e)
            {
                Platform.runLater(() ->
                {
                    showStatus("Export failed: " + e.getMessage(), "status-error");
                    updateButtonState();
                });
            }
        }, threadName);

        exportThread.setDaemon(true);
        exportThread.start();
    }

    private void ensureImagesLoaded(List<Document> documents) throws SQLException
    {
        if (archiveService == null) { return; }

        for (Document document : documents)
        {
            for (Files page : document.getPages())
            {
                if (!page.isBarcode() && page.getImage() == null && page.getId() > 0)
                {
                    page.setImage(archiveService.loadFileImage(page.getId()));
                }
            }
        }
    }

    private String buildExportFolderName()
    {
        String  boxId   = selectedBox == null ? "export" : selectedBox.getBoxId();
        Profile profile = cmbProfile.getValue();

        if (profile != null && !profile.getName().isBlank())
        {
            String safeName = profile.getName().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
            return safeName + "_" + boxId;
        }

        return boxId;
    }
}
