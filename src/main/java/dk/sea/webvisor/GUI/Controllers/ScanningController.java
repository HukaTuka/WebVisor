package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.*;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.BE.UserRole;

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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import javafx.stage.DirectoryChooser;
import java.io.File;

public class ScanningController {
    private static final String BARCODE_CELL_STYLE = "barcode-item-cell";
    private static final Client NO_CLIENT_OPTION = new Client(-1, "-- No client selected --");

    @FXML
    private TextField txtBoxId;
    @FXML
    private ComboBox<Client> cmbClient;
    @FXML
    private Label lblCurrentBox;
    @FXML
    private Label lblExplorerPath;
    @FXML
    private Button btnBack;
    @FXML
    private TreeView<Object> treeExplorer;
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
    @FXML
    private ComboBox<Profile> cmbProfile;
    @FXML
    private Button btnExportSingle;
    @FXML
    private Button btnExportMulti;
    @FXML
    private Button btnSplit;

    private final ScanningService scanningService = new ScanningService();
    private UserService userService;
    private ProfileUserService profileUserService;
    private final AuditService audit = AuditService.getInstance();
    private final ObservableList<Files> pageItems = FXCollections.observableArrayList();
    private final ObservableList<Boxes> boxItems = FXCollections.observableArrayList();
    private final Set<String> loadedBoxContent = new HashSet<>();
    private final Map<String, TreeItem<Object>> boxNodesById = new HashMap<>();
    private Files draggedPage = null;
    private ArchiveService archiveService;
    private final ExportService exportService = new ExportService();
    private ProfileService profileService;

    private volatile boolean running = false;
    private Thread pollingThread = null;
    private int currentIndex = -1;
    private Boxes selectedBox = null;

    /**
     * Interval between API polls while a session is active (milliseconds).
     */
    private static final long POLL_INTERVAL_MS = 0;


    @FXML
    private void initialize() {
        try {
            archiveService = new ArchiveService();
            boxItems.setAll(archiveService.getAllBoxes());
            List<Client> clients = new ArrayList<>();
            clients.add(NO_CLIENT_OPTION);
            clients.addAll(archiveService.getAllClients());
            cmbClient.setItems(FXCollections.observableArrayList(clients));
            cmbClient.setValue(NO_CLIENT_OPTION);
        } catch (IOException | SQLException e) {
            showStatus("Could not load data from database: " + e.getMessage(), "status-error");
        }

        cmbClient.valueProperty().addListener((obs, oldClient, newClient) ->
        {
            selectedBox = null;
            pageItems.clear();
            scanningService.clearSession();
            clearImagePreview();
            updateTotalScansLabel();
            updateCurrentBoxLabel();
            rebuildExplorerTree();
        });

        try {
            profileService = new ProfileService();
            userService = new UserService();
            profileUserService = new ProfileUserService();

            List<Profile> profilesForDropdown = new ArrayList<>(profileService.getAllProfiles());

            String currentUsername = audit.getCurrentUser();
            if (currentUsername != null && !currentUsername.isBlank())
            {
                Optional<User> currentUser = userService.getUserByUsername(currentUsername);
                if (currentUser.isPresent() && currentUser.get().getRole() != UserRole.UserAdmin)
                {
                    List<Profile> assignedProfiles =
                            profileUserService.getProfilesForUser(currentUser.get().getId());
                    if (!assignedProfiles.isEmpty())
                    {
                        profilesForDropdown = assignedProfiles;
                    }
                }
            }
            cmbProfile.setItems(FXCollections.observableArrayList(profilesForDropdown));
        }  catch (IOException | SQLException e){
            showStatus("Could not load profiles: " + e.getMessage(), "status-error");
        }

        treeExplorer.setCellFactory(tv -> new TreeCell<>() {
            {
                setOnDragDetected(event ->
                {
                    if (!canReorderPages() || isEmpty() || !(getItem() instanceof Files page))
                    {
                        return;
                    }

                    draggedPage = page;
                    Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("page-reorder");
                    dragboard.setContent(content);
                    event.consume();
                });

                setOnDragOver(event ->
                {
                    if (!canReorderPages() || draggedPage == null || isEmpty() || !(getItem() instanceof Files))
                    {
                        return;
                    }

                    if (getItem() != draggedPage)
                    {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });

                setOnDragDropped(event ->
                {
                    boolean completed = false;
                    if (canReorderPages() && draggedPage != null && !isEmpty() && getItem() instanceof Files targetPage)
                    {
                        completed = reorderPage(draggedPage, targetPage);
                    }
                    event.setDropCompleted(completed);
                    event.consume();
                });

                setOnDragDone(event -> draggedPage = null);
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove(BARCODE_CELL_STYLE);

                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    return;
                }

                if (item instanceof Boxes box) {
                    String documentsText = box.getDocumentCount() == 1 ? "1 document" : box.getDocumentCount() + " documents";
                    String filesText = box.getFileCount() == 1 ? "1 file" : box.getFileCount() + " files";
                    setText(box.getBoxId() + " (" + documentsText + ", " + filesText + ")");
                    setContextMenu(null);
                } else if (item instanceof Document document) {
                    setText(document.toString());
                    setContextMenu(null);
                } else if (item instanceof Files page) {
                    if (page.isBarcode()) {
                        setText(page.getReferenceId() + " [BARCODE]");
                        if (!getStyleClass().contains(BARCODE_CELL_STYLE)) {
                            getStyleClass().add(BARCODE_CELL_STYLE);
                        }
                    } else {
                        setText(page.getReferenceId());
                    }

                    // ── Right-click context menu: Move to Document X ──────────
                    setContextMenu(buildMoveContextMenu(page));
                }
            }
        });

        treeExplorer.setOnMouseClicked(event ->
        {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            Object selected = getSelectedExplorerValue();
            if (selected instanceof Files && event.getClickCount() == 1) {
                openSelectedItem();
                return;
            }

            if (event.getClickCount() == 2) {
                openSelectedItem();
            }
        });
        treeExplorer.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> updateButtonState());

        rebuildExplorerTree();
        updateCurrentBoxLabel();
        updateTotalScansLabel();
        updatePageLabel();
        updateButtonState();
    }

    /**
     * Builds a context menu for a Files row with one "Move to Document N"
     * item per document that the page does not already belong to.
     * Returns null (no menu) when not at FILES level, scanning is running,
     * or there is only one document.
     */
    private ContextMenu buildMoveContextMenu(Files page)
    {
        if (running || selectedBox == null)
        {
            return null;
        }

        List<Document> docs = scanningService.getDocuments();
        if (docs.size() <= 1)
        {
            return null; // nothing to move to
        }

        // Find which document currently owns this page
        int sourceDocIndex = -1;
        outer:
        for (int i = 0; i < docs.size(); i++)
        {
            for (Files p : docs.get(i).getPages())
            {
                if (p == page) { sourceDocIndex = i; break outer; }
            }
        }

        ContextMenu menu = new ContextMenu();
        for (int i = 0; i < docs.size(); i++)
        {
            if (i == sourceDocIndex) continue; // skip current document

            final int targetIndex = i;
            MenuItem item = new MenuItem("Move to " + docs.get(i));
            item.setOnAction(e -> handleMoveToDocument(page, targetIndex));
            menu.getItems().add(item);
        }

        return menu.getItems().isEmpty() ? null : menu;
    }

    /**
      moves page to document persists the change, refreshes the list, and writes an audit entry.
     */
    private void handleMoveToDocument(Files page, int targetDocumentIndex)
    {
        int pageIndex = findPageIndex(page);
        if (pageIndex < 0)
        {
            showStatus("Could not find page to move.", "status-error");
            return;
        }

        boolean moved = scanningService.movePageToDocument(pageIndex, targetDocumentIndex);
        if (!moved)
        {
            showStatus("Could not move page — it may already be in that document.", "status-error");
            return;
        }

        pageItems.setAll(scanningService.getAllPages());
        updateBoxSnapshotFromCurrentSession();

        if (archiveService != null)
        {
            try
            {
                archiveService.saveBoxSnapshot(selectedBox);
            }
            catch (SQLException e)
            {
                showStatus("Moved in memory but could not persist to database: "
                        + e.getMessage(), "status-error");
            }
        }

        showFilesLevel();

        audit.log("PAGE_MOVED_TO_DOCUMENT",
                "Moved " + page.getReferenceId()
                        + " to Document " + (targetDocumentIndex + 1)
                        + " in box " + selectedBox.getBoxId());

        showStatus("Moved " + page.getReferenceId()
                + " → Document " + (targetDocumentIndex + 1), "status-success");
    }

    @FXML
    private void onCreateBox() {
        String boxId = txtBoxId.getText() == null ? "" : txtBoxId.getText().trim();
        Client selectedClient = cmbClient.getValue();
        String client = selectedClient == null ? "" : selectedClient.getName().trim();

        if (boxId.isEmpty()) {
            showStatus("Please enter a Box ID.", "status-error");
            return;
        }
        if (selectedClient == null || selectedClient.getId() <= 0) {
            showStatus("Please fill Client.", "status-error");
            return;
        }

        for (Boxes box : boxItems) {
            if (box.getBoxId().equalsIgnoreCase(boxId)) {
                showStatus("Box ID already exists.", "status-error");
                return;
            }
        }

        Boxes createdBox = null;
        if (archiveService != null) {
            try {
                createdBox = archiveService.createBox(boxId, client);
            } catch (SQLException e) {
                showStatus("Could not create box in database: " + e.getMessage(), "status-error");
                return;
            }
            catch (IllegalArgumentException e)
            {
                showStatus(e.getMessage(), "status-error");
                return;
            }
        }

        Boxes newBox = createdBox != null
                ? createdBox
                : new Boxes(boxId, client);
        boxItems.add(newBox);
        loadedBoxContent.add(boxId);
        txtBoxId.clear();
        rebuildExplorerTree();
        selectExplorerValue(newBox);
        showStatus("Created box " + boxId + ". Click it to open.", "status-success");
        audit.log("BOX_CREATED", "Created box " + boxId + " (client: " + client + ")");
    }

    @FXML
    private void onExportSinglePage() {
        performExport(true);
    }

    @FXML
    private void onExportMultiPage() {
        performExport(false);
    }

    @FXML
    private void onBack() {
        TreeItem<Object> selectedNode = treeExplorer.getSelectionModel().getSelectedItem();
        if (selectedNode == null) {
            return;
        }
        TreeItem<Object> parent = selectedNode.getParent();
        if (parent != null && parent.getValue() != null) {
            treeExplorer.getSelectionModel().select(parent);
            if (parent.getValue() instanceof Boxes) {
                selectedBox = (Boxes) parent.getValue();
                pageItems.setAll(selectedBox.getPages());
            } else if (!(parent.getValue() instanceof Files)) {
                selectedBox = null;
                pageItems.clear();
            }
            clearImagePreview();
            updateCurrentBoxLabel();
            updateButtonState();
        }
    }

    @FXML
    private void onStartScanning() {
        if (running) {
            return;
        }

        if (cmbProfile.getValue() == null) {
            showStatus("Select a Profile before starting a scan.", "status-error");
            return;
        }

        if (selectedBox == null) {
            showStatus("Open a box before scanning.", "status-error");
            return;
        }

        if (!pageItems.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Start a new session in this box? This will clear current files in the box.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
        }

        scanningService.clearSession();
        selectedBox.clearContent();
        pageItems.clear();
        currentIndex = -1;
        if (imgPage != null) {
            imgPage.setImage(null);
        }
        updateTotalScansLabel();
        updatePageLabel();
        rebuildExplorerTree();
        showStatus("Scanning started — polling API every " + POLL_INTERVAL_MS / 1000 + " s…", "status-info");
        audit.log("SCAN_STARTED", "Scanning started in box " + selectedBox.getBoxId());

        running = true;
        updateButtonState();

        pollingThread = new Thread(this::pollLoop, "scanning-poll-thread");
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    @FXML
    private void onStopScanning() {
        running = false;
        updateButtonState();
        updateBoxSnapshotFromCurrentSession();

        if (archiveService != null && selectedBox != null) {
            try {
                archiveService.saveBoxSnapshot(selectedBox);
            } catch (SQLException e) {
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
    private void onPrev() {
        if (currentIndex > 0) {
            navigateTo(currentIndex - 1);
        }
    }

    @FXML
    private void onNext() {
        if (currentIndex < pageItems.size() - 1) {
            navigateTo(currentIndex + 1);
        }
    }


    @FXML
    private void onRotateLeft() {
        applyRotation(false);
    }

    @FXML
    private void onRotateRight() {
        applyRotation(true);
    }

    @FXML
    public void onDelete(ActionEvent actionEvent) {
        if (running) {
            showStatus("Stop scanning before deleting items.", "status-error");
            return;
        }

        Object selected = getSelectedExplorerValue();
        if (selected == null) {
            showStatus("No item selected for deletion.", "status-error");
            return;
        }

        try
        {
            if (selected instanceof Boxes box)
            {
                if (archiveService != null) {
                    archiveService.deleteBox(box.getBoxId());
                }
                boxItems.remove(box);
                loadedBoxContent.remove(box.getBoxId());
                if (selectedBox != null && selectedBox.getBoxId().equalsIgnoreCase(box.getBoxId())) {
                    selectedBox = null;
                    pageItems.clear();
                    scanningService.clearSession();
                    clearImagePreview();
                    updateTotalScansLabel();
                    updateCurrentBoxLabel();
                }
                rebuildExplorerTree();
                showStatus("Box deleted: " + box.getBoxId(), "status-success");
                audit.log("BOX_DELETED", "Deleted box " + box.getBoxId());
                return;
            }

            if (selectedBox == null && (selected instanceof Document || selected instanceof Files)) {
                TreeItem<Object> selectedNode = treeExplorer.getSelectionModel().getSelectedItem();
                Boxes owner = selectedNode == null ? null : getOwnerBoxForNode(selectedNode);
                if (owner != null) {
                    selectedBox = owner;
                    pageItems.setAll(owner.getPages());
                    scanningService.loadSessionPages(pageItems);
                    updateCurrentBoxLabel();
                    updateTotalScansLabel();
                }
            }

            if (selectedBox == null)
            {
                showStatus("Open a box before deleting documents or files.", "status-error");
                return;
            }

            if (selected instanceof Document document)
            {
                if (archiveService != null) {
                    for (Files documentPage : document.getPages()) {
                        if (documentPage.getId() > 0) {
                            archiveService.deleteFileById(documentPage.getId());
                        }
                    }
                    archiveService.deleteDocumentByNumber(selectedBox.getBoxId(), document.getDocumentNumber());
                }

                List<Files> remaining = new ArrayList<>();
                for (Files page : pageItems) {
                    if (!document.getPages().contains(page)) {
                        remaining.add(page);
                    }
                }
                scanningService.loadSessionPages(remaining);
                pageItems.setAll(scanningService.getAllPages());
                clearImagePreview();
                updateTotalScansLabel();
                updateBoxSnapshotFromCurrentSession();
                if (archiveService != null) {
                    archiveService.updatePageOrder(selectedBox.getBoxId(), pageItems);
                }
                rebuildExplorerTree();
                showStatus("Deleted " + document, "status-success");
                audit.log("DOCUMENT_DELETED", "Deleted " + document + " in box " + selectedBox.getBoxId());
                return;
            }

            if (selected instanceof Files page)
            {
                int deleteIndex = findPageIndex(page);
                if (deleteIndex < 0) {
                    showStatus("Could not delete selected file.", "status-error");
                    return;
                }

                if (archiveService != null && page.getId() > 0) {
                    archiveService.deleteFileById(page.getId());
                }

                if (!scanningService.deletePageAt(deleteIndex)) {
                    showStatus("Could not delete selected file.", "status-error");
                    return;
                }

                pageItems.setAll(scanningService.getAllPages());
                if (!pageItems.isEmpty()) {
                    navigateTo(Math.min(deleteIndex, pageItems.size() - 1));
                } else {
                    clearImagePreview();
                }

                updateTotalScansLabel();
                updateBoxSnapshotFromCurrentSession();
                if (archiveService != null) {
                    archiveService.updatePageOrder(selectedBox.getBoxId(), pageItems);
                }
                rebuildExplorerTree();
                showStatus("Deleted file: " + page.getReferenceId(), "status-success");
                audit.log("PAGE_DELETED", "Deleted file " + page.getReferenceId() + " in box " + selectedBox.getBoxId());
                return;
            }

            showStatus("Select a valid item to delete.", "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not delete item from database: " + e.getMessage(), "status-error");
        }
    }

    private void pollLoop() {
        while (running) {
            try {
                List<Files> newPages = scanningService.fetchAndAppendNext();

                Platform.runLater(() -> handleNewPages(newPages));

                Thread.sleep(POLL_INTERVAL_MS);
            } catch (IOException e) {
                Platform.runLater(() ->
                        showStatus("API error: " + e.getMessage() + " — retrying…", "status-error"));

                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleNewPages(List<Files> newPages) {
        if (newPages.isEmpty()) {
            return;
        }

        //applies the rotation of the profile selected
        Profile activeProfile = cmbProfile.getValue();
        if (activeProfile != null && activeProfile.getDefaultRotation() != 0) {
            for (Files page : newPages) {
                page.setRotationDegrees(activeProfile.getDefaultRotation());
            }
        }
        pageItems.addAll(newPages);
        navigateTo(pageItems.size() - 1);
        updateTotalScansLabel();
        updateBoxSnapshotFromCurrentSession();

        audit.log("NEW_PAGES", "Page at index " + pageItems.size() + " was created");

        rebuildExplorerTree();

        boolean barcodeFound = newPages.stream().anyMatch(Files::isBarcode);
        if (barcodeFound) {
            int splitPage = newPages.stream()
                    .filter(Files::isBarcode)
                    .mapToInt(Files::getPageNumber)
                    .min()
                    .orElse(-1);

            //Audit: barcode / document split detected
            audit.log("BARCODE_DETECTED", "Document split barcode detected at page " + splitPage);

            //Only auto splits if the profile has the split on barcode enabled
            boolean shouldAutoSplit = activeProfile != null && activeProfile.isSplitOnBarcode();
            if (shouldAutoSplit && selectedBox != null) {
                int barcodeOffset = newPages.stream()
                        .filter(Files::isBarcode)
                        .mapToInt(newPages::indexOf)
                        .min()
                        .orElse(0);
                int absoluteSplitIndex = (pageItems.size() - newPages.size()) + barcodeOffset;

                if(absoluteSplitIndex > 0 && absoluteSplitIndex < pageItems.size() -1) {
                    scanningService.splitDocumentAt(absoluteSplitIndex);
                    updateBoxSnapshotFromCurrentSession();
                    audit.log("AUTO_SPLIT", "Auto-split triggered by a barcode at page " + splitPage);
                }
            }

            showStatus("⚠ Barcode detected — document split at page " +
                            newPages.stream().filter(Files::isBarcode)
                                    .mapToInt(Files::getPageNumber).min().orElse(-1),
                    "status-error");
        } else {
            showStatus("Page " + newPages.get(0).getPageNumber() + " received.", "status-info");
        }
    }

    private void openSelectedItem() {
        Object selected = getSelectedExplorerValue();
        TreeItem<Object> selectedNode = treeExplorer.getSelectionModel().getSelectedItem();
        if (selected == null || selectedNode == null) {
            return;
        }

        if (running && !(selected instanceof Files)) {
            showStatus("Stop scanning before changing box/document.", "status-error");
            return;
        }

        if (selected instanceof Boxes box) {
            if (archiveService != null && !loadedBoxContent.contains(box.getBoxId())) {
                try {
                    Boxes hydrated = archiveService.loadBoxContent(box.getBoxId());
                    box.replaceContent(hydrated.getPages(), hydrated.getDocuments());
                    loadedBoxContent.add(box.getBoxId());
                } catch (SQLException e) {
                    showStatus("Could not load box content: " + e.getMessage(), "status-error");
                    return;
                }
            }

            selectedBox = box;
            pageItems.setAll(box.getPages());
            scanningService.loadSessionPages(pageItems);
            clearImagePreview();
            updateCurrentBoxLabel();
            updateTotalScansLabel();
            populateBoxNode(selectedNode, box);
            selectedNode.setExpanded(true);
            showStatus("Opened box " + box.getBoxId(), "status-info");
            audit.log("BOX_OPENED", "Opened box " + box.getBoxId());
            return;
        }

        if (selected instanceof Document document) {
            Boxes owner = getOwnerBoxForNode(selectedNode);
            if (owner != null && (selectedBox == null || !selectedBox.getBoxId().equalsIgnoreCase(owner.getBoxId()))) {
                selectedBox = owner;
                pageItems.setAll(owner.getPages());
                scanningService.loadSessionPages(pageItems);
                updateCurrentBoxLabel();
                updateTotalScansLabel();
            }
            populateDocumentNode(selectedNode, document);
            selectedNode.setExpanded(!selectedNode.isExpanded());
            clearImagePreview();
            showStatus("Opened " + document, "status-info");
            return;
        }

        if (selected instanceof Files page) {
            int index = findPageIndex(page);
            if (index >= 0) {
                navigateTo(index);
            }
        }
    }

    private int findPageIndex(Files selectedPage) {
        if (selectedPage.getId() > 0) {
            for (int i = 0; i < pageItems.size(); i++) {
                if (pageItems.get(i).getId() == selectedPage.getId()) {
                    return i;
                }
            }
        }

        return pageItems.indexOf(selectedPage);
    }

    private boolean canReorderPages()
    {
        Object selected = getSelectedExplorerValue();
        return !running
                && selectedBox != null
                && selected instanceof Files;
    }

    private boolean reorderPage(Files sourcePage, Files targetPage)
    {
        int fromIndex = findPageIndex(sourcePage);
        int toIndex = findPageIndex(targetPage);
        if (fromIndex < 0 || toIndex < 0)
        {
            return false;
        }

        if (!scanningService.movePage(fromIndex, toIndex))
        {
            return false;
        }

        pageItems.setAll(scanningService.getAllPages());
        updateBoxSnapshotFromCurrentSession();
        rebuildExplorerTree();

        int newIndex = findPageIndex(sourcePage);
        if (newIndex >= 0)
        {
            navigateTo(newIndex);
        }

        if (!persistPageOrder())
        {
            return false;
        }

        audit.log("PAGES_REORDERED", "Moved page from index " + fromIndex + " to " + toIndex + " in box " + selectedBox.getBoxId());
        showStatus("Page order updated.", "status-success");
        return true;
    }

    private boolean persistPageOrder()
    {
        if (archiveService == null || selectedBox == null)
        {
            return true;
        }

        try
        {
            boolean hasPersistedIds = pageItems.stream().anyMatch(page -> page.getId() > 0);
            if (hasPersistedIds)
            {
                archiveService.updatePageOrder(selectedBox.getBoxId(), pageItems);
            }
            else
            {
                archiveService.saveBoxSnapshot(selectedBox);
            }
            return true;
        }
        catch (SQLException e)
        {
            showStatus("Could not update page order in database: " + e.getMessage(), "status-error");
            return false;
        }
    }

    private void showBoxesLevel() { rebuildExplorerTree(); }
    private void showDocumentsLevel() { rebuildExplorerTree(); }
    private void showFilesLevel() { rebuildExplorerTree(); }

    private void navigateTo(int index) {
        if (index < 0 || index >= pageItems.size()) {
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

        if (page.getImage() == null) {
            if (archiveService == null || page.getId() <= 0) {
                showStatus("Could not load image for selected file.", "status-error");
                return;
            }

            try {
                page.setImage(archiveService.loadFileImage(page.getId()));
            } catch (SQLException e) {
                showStatus("Could not load image: " + e.getMessage(), "status-error");
                return;
            }
        }

        BufferedImage raw = page.getImage();
        BufferedImage rotated = applyAwtRotation(raw, page.getRotationDegrees());
        Image fxImage = SwingFXUtils.toFXImage(rotated, null);

        imgPage.setImage(fxImage);
        imgPage.setOpacity(page.isBarcode() ? 0.55 : 1.0);
        imgPage.setRotate(0);
    }

    private BufferedImage applyAwtRotation(BufferedImage src, int degrees) {
        if (degrees == 0) {
            return src;
        }

        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

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

    private void applyRotation(boolean clockwise) {
        if (currentIndex < 0 || currentIndex >= pageItems.size()) {
            return;
        }

        Files page = pageItems.get(currentIndex);
        if (clockwise) {
            page.rotateRight();
        } else {
            page.rotateLeft();
        }

        displayPage(page);
    }

    private void updatePageLabel() {
        int total = pageItems.size();
        int shown = total == 0 ? 0 : currentIndex + 1;
        lblPageInfo.setText("Page " + shown + " / " + total);
    }

    private void updateNavigationButtons() {
        btnPrev.setDisable(currentIndex <= 0);
        btnNext.setDisable(currentIndex >= pageItems.size() - 1 || pageItems.isEmpty());
    }

    private void clearImagePreview()
    {
        currentIndex = -1;
        if (imgPage != null) {
            imgPage.setImage(null);
        }
        updatePageLabel();
        updateNavigationButtons();
    }

    private void updateButtonState() {
        boolean hasPage = currentIndex >= 0 && currentIndex < pageItems.size();
        Object selected = getSelectedExplorerValue();
        boolean canDelete = !running && (selected instanceof Boxes || selected instanceof Document || selected instanceof Files);

        btnStart.setDisable(running || selectedBox == null);
        btnStop.setDisable(!running);
        btnSplit.setDisable(!hasPage || currentIndex >= pageItems.size() - 1);

        btnRotateLeft.setDisable(!hasPage);
        btnRotateRight.setDisable(!hasPage);
        btnDelete.setDisable(!canDelete);
        btnBack.setDisable(treeExplorer == null || treeExplorer.getSelectionModel().getSelectedItem() == null);
        boolean canExport = !running
                && selectedBox != null
                && !selectedBox.getDocuments().isEmpty();
        btnExportSingle.setDisable(!canExport);
        btnExportMulti.setDisable(!canExport);
        updateNavigationButtons();
    }

    private void updateTotalScansLabel() {
        lblTotalScans.setText("Total Scans: " + pageItems.size());
    }

    private void updateCurrentBoxLabel() {
        lblCurrentBox.setText(selectedBox == null ? "Current box: none" : "Current box: " + selectedBox.getBoxId());
    }

    private void updateBoxSnapshotFromCurrentSession() {
        if (selectedBox == null) {
            return;
        }

        selectedBox.replaceContent(scanningService.getAllPages(), scanningService.getDocuments());
        rebuildExplorerTree();
    }

    private void rebuildExplorerTree()
    {
        TreeItem<Object> root = new TreeItem<>();
        root.setExpanded(true);
        boxNodesById.clear();

        Client selectedClient = cmbClient == null ? null : cmbClient.getValue();
        for (Boxes box : boxItems)
        {
            if (selectedClient == null || selectedClient.getId() <= 0 || !selectedClient.getName().equalsIgnoreCase(box.getClient())) {
                continue;
            }
            TreeItem<Object> boxNode = new TreeItem<>(box);
            if (selectedBox != null && selectedBox.getBoxId().equalsIgnoreCase(box.getBoxId()))
            {
                populateBoxNode(boxNode, box);
                boxNode.setExpanded(true);
            }
            boxNodesById.put(box.getBoxId(), boxNode);
            root.getChildren().add(boxNode);
        }

        treeExplorer.setRoot(root);
        treeExplorer.setShowRoot(false);
        lblExplorerPath.setText("Boxes / Documents / Files");
        btnBack.setDisable(treeExplorer.getSelectionModel().getSelectedItem() == null);
        updateButtonState();
    }

    private void populateBoxNode(TreeItem<Object> boxNode, Boxes box)
    {
        boxNode.getChildren().clear();
        for (Document document : box.getDocuments())
        {
            boxNode.getChildren().add(new TreeItem<>(document));
        }
    }

    private void populateDocumentNode(TreeItem<Object> documentNode, Document document)
    {
        documentNode.getChildren().clear();
        for (Files page : document.getPages())
        {
            documentNode.getChildren().add(new TreeItem<>(page));
        }
    }

    private Object getSelectedExplorerValue()
    {
        TreeItem<Object> item = treeExplorer == null ? null : treeExplorer.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getValue();
    }

    private Boxes getOwnerBoxForNode(TreeItem<Object> node)
    {
        TreeItem<Object> cursor = node;
        while (cursor != null)
        {
            if (cursor.getValue() instanceof Boxes box)
            {
                return box;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private void selectExplorerValue(Object value)
    {
        if (value instanceof Boxes box)
        {
            TreeItem<Object> node = boxNodesById.get(box.getBoxId());
            if (node != null)
            {
                treeExplorer.getSelectionModel().select(node);
            }
        }
    }

    private void showStatus(String message, String styleClass) {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }

    @FXML
    private void onOpenSlideView() {
        if (selectedBox == null) {
            showStatus("Open a box first.", "status-error");
            return;
        }

        List<Files> savedPages = selectedBox.getPages();

        if (savedPages.isEmpty()) {
            showStatus("No saved pages in this box.", "status-error");
            return;
        }

        if (archiveService != null) {
            for (Files page : savedPages) {
                if (page.getImage() == null && page.getId() > 0) {
                    try {
                        page.setImage(archiveService.loadFileImage(page.getId()));
                    } catch (SQLException e) {
                        showStatus("Could not load image for " + page.getReferenceId() + ": " + e.getMessage(), "status-error");
                        return;
                    }
                }
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/SlideView.fxml"));
            Parent root = loader.load();

            SlideViewController controller = loader.getController();
            controller.setPages(savedPages, Math.max(currentIndex, 0));

            Stage stage = new Stage();
            stage.setTitle("Slide View — " + selectedBox.getBoxId());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(imgPage.getScene().getWindow());
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.show();

            audit.log("SLIDE_VIEW_OPENED", "Opened slide view for box "
                    + selectedBox.getBoxId() + " — " + savedPages.size() + " page(s)");
        } catch (IOException e) {
            showStatus("Could not open slide view.", "status-error");
        }
    }

    private void performExport(boolean singlePage) {
        if (selectedBox == null || selectedBox.getDocuments().isEmpty()) {
            showStatus("No documents to export. Complete and stop a scanning session first.", "status-error");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Export Output Directory");

        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.exists() && downloads.isDirectory()) {
            chooser.setInitialDirectory(downloads);
        }

        File outputDirectory = chooser.showDialog(btnExportSingle.getScene().getWindow());

        if (outputDirectory == null) {
            return;
        }

        String folderName = buildExportFolderName();
        List<Document> documents = new ArrayList<>(selectedBox.getDocuments());

        btnExportSingle.setDisable(true);
        btnExportMulti.setDisable(true);
        showStatus("Exporting — please wait…", "status-info");

        String threadName = singlePage ? "export-single-page-thread" : "export-multi-page-thread";
        Thread exportThread = new Thread(() ->
        {
            try {
                ensureImagesLoaded(documents);

                if (singlePage) {
                    int count = exportService.exportSinglePage(documents, outputDirectory, folderName);
                    String message = "Single-page export complete: " + count
                            + " file(s) written to \"" + folderName + "\".";
                    Platform.runLater(() ->
                    {
                        showStatus(message, "status-success");
                        audit.log("EXPORT_SINGLE", message);
                        updateButtonState();
                    });
                } else {
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
            } catch (SQLException e) {
                Platform.runLater(() ->
                {
                    showStatus("Could not load image data for export: " + e.getMessage(), "status-error");
                    updateButtonState();
                });
            } catch (IOException e) {
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

    private void ensureImagesLoaded(List<Document> documents) throws SQLException {
        if (archiveService == null) {
            return;
        }

        for (Document document : documents) {
            for (Files page : document.getPages()) {
                if (!page.isBarcode() && page.getImage() == null && page.getId() > 0) {
                    page.setImage(archiveService.loadFileImage(page.getId()));
                }
            }
        }
    }

    private String buildExportFolderName() {
        String boxId = selectedBox == null ? "export" : selectedBox.getBoxId();
        Profile profile = cmbProfile.getValue();

        if (profile != null && !profile.getName().isBlank()) {
            String safeName = profile.getName().trim().replaceAll("[\\\\/:*?\"<>|]", "_");
            return safeName + "_" + boxId;
        }

        return boxId;
    }

    @FXML
    private void onSplitDocument() {
        if (currentIndex < 0 || currentIndex >= pageItems.size() - 1) {
            showStatus("Select a page to split after — it cannot be the last page.", "status-error");
            return;
        }

        if (selectedBox == null) {
            showStatus("No box is open.", "status-error");
            return;
        }

        Files splitAfterPage = pageItems.get(currentIndex);

        List<Files> boxPages = new ArrayList<>(selectedBox.getPages());
        int boxPageIndex = -1;
        for (int i = 0; i < boxPages.size(); i++) {
            Files p = boxPages.get(i);
            if (p.getId() > 0 && p.getId() == splitAfterPage.getId()) {
                boxPageIndex = i;
                break;
            }
            if (p.getPageNumber() == splitAfterPage.getPageNumber()) {
                boxPageIndex = i;
            }
        }

        if (boxPageIndex < 0 || boxPageIndex >= boxPages.size() - 1) {
            showStatus("Could not find page position for split.", "status-error");
            return;
        }

        try {
            if (archiveService != null) {
                archiveService.splitDocumentAt(selectedBox, boxPageIndex);
            } else {
                scanningService.splitDocumentAt(currentIndex);
                updateBoxSnapshotFromCurrentSession();
            }

            showDocumentsLevel();

            audit.log("MANUAL_SPLIT", "Manual document split inserted after "
                    + splitAfterPage.getReferenceId()
                    + " in box " + selectedBox.getBoxId());

            showStatus("Split applied after " + splitAfterPage.getReferenceId()
                            + " — " + selectedBox.getDocuments().size() + " document(s) now in box.",
                    "status-success");
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), "status-error");
        } catch (SQLException e) {
            showStatus("Could not save split to database: " + e.getMessage(), "status-error");
        }
    }
}
