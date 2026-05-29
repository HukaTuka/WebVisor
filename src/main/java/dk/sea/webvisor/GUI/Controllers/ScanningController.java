package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.*;
import dk.sea.webvisor.BLL.*;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.DAL.Interface.AuditAware;
import dk.sea.webvisor.GUI.Managers.*;

// Java Imports
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ScanningController implements AuditAware
{
    public enum Level { BOXES, DOCUMENTS, FILES }
    private static final Client NO_CLIENT_OPTION = new Client(-1, "No client selected");

    @FXML private TextField txtBoxId;
    @FXML private AnchorPane rootPane;
    @FXML private ComboBox<Client>   cmbClient;
    @FXML private ComboBox<Archive>  cmbArchive;
    @FXML private ComboBox<Profile>  cmbProfile;
    @FXML private ComboBox<Integer>  cmbSessionRotation;
    @FXML private TreeView<Object>   treeExplorer;
    @FXML private Label  lblStatus, lblPageInfo, lblTotalScans, lblCurrentBox, lblExplorerPath;
    @FXML private Button btnStart, btnStop, btnPrev, btnNext, btnRotateLeft, btnRotateRight;
    @FXML private Button btnDelete, btnSplit, btnBack, btnExportSingle, btnExportMulti;
    @FXML private Button btnMetadata, btnSlideView, btnMarkQA;
    @FXML private Button btnSettings;
    @FXML private ImageView imgPage;

    private ArchiveService archiveService;
    private ScanningService scanningService;
    private ExportService exportService;
    private ProfileService profileService;
    private ProfileUserService profileUserService;
    private UserService userService;
    private BoxMetadataService boxMetadataService;

    private AuditService audit;
    private UiManager uiManager;
    private PageNavigationManager navigation;
    private PageViewerManager pageViewerManager;
    private ScanPollingManager polling;
    private ExportManager exporter;
    private BoxSplitManager splitter;
    private DeleteManager deleteManager;
    private ExplorerTreeManager explorerTreeManager;
    private ShortcutManager shortcutManager;
    private ProfileDropdownManager profileDropdownManager;
    private ComboBoxKeyboardManager comboBoxKeyboardManager;


    private final List<Boxes>   allBoxes    = new ArrayList<>();
    private final List<Archive> allArchives = new ArrayList<>();
    private ScanningSessionManager sessionManager;

    @Override
    public void setAudit(AuditService audit){
        this.audit = audit;
        initServices();
        initHelpers();
        loadInitialDropdowns();
        comboBoxKeyboardManager.configure(cmbClient, cmbArchive);
        comboBoxKeyboardManager.configure(cmbClient, cmbArchive);
        comboBoxKeyboardManager.configure(cmbProfile, cmbSessionRotation);
        comboBoxKeyboardManager.configure(cmbSessionRotation, txtBoxId);
        setupTreeView();
        setupShortcuts();
        showBoxes();
        updateUI();
    }


    private void initServices()
    {
        try
        {
            archiveService    = new ArchiveService();
            scanningService   = new ScanningService();
            exportService     = new ExportService();
            profileService    = new ProfileService();
            profileUserService = new ProfileUserService();
            userService       = new UserService();
            boxMetadataService = new BoxMetadataService();

            allBoxes.clear();
            allBoxes.addAll(archiveService.getAllBoxes());
            allArchives.clear();
            allArchives.addAll(archiveService.getAllArchives());
        }
        catch (Exception e)
        {
            e.printStackTrace();

        }
    }

    private void initHelpers()
    {
        uiManager = new UiManager(lblStatus, archiveService);
        pageViewerManager = new PageViewerManager(archiveService, uiManager);
        navigation        = new PageNavigationManager(imgPage, lblPageInfo, pageViewerManager);
        polling           = new ScanPollingManager(scanningService);
        exporter          = new ExportManager(exportService, archiveService, uiManager, audit);
        splitter          = new BoxSplitManager(archiveService, scanningService, uiManager, audit);
        deleteManager     = new DeleteManager(archiveService, scanningService, uiManager, audit);
        explorerTreeManager = new ExplorerTreeManager(treeExplorer, uiManager, this::onTreeSelectionChanged);
        sessionManager    = new ScanningSessionManager();
        profileDropdownManager = new ProfileDropdownManager(profileService, profileUserService, userService, audit, cmbProfile, cmbSessionRotation, uiManager);
        comboBoxKeyboardManager = new ComboBoxKeyboardManager();
    }


    private void loadInitialDropdowns()
    {
        cmbSessionRotation.setItems(FXCollections.observableArrayList(0, 90, 180, 270));
        cmbSessionRotation.setValue(0);
        cmbSessionRotation.setConverter(new javafx.util.StringConverter<>()
        {
            @Override
            public String toString(Integer value)
            {
                return value == null ? "0\u00b0" : value + "\u00b0";
            }

            @Override
            public Integer fromString(String string)
            {
                if (string == null) return 0;
                try { return Integer.parseInt(string.replace("\u00b0", "").trim()); }
                catch (NumberFormatException e) { return 0; }
            }
        });

        try
        {
            List<Client> clients = new ArrayList<>();
            clients.add(NO_CLIENT_OPTION);
            clients.addAll(archiveService.getAllClients());
            cmbClient.setItems(FXCollections.observableArrayList(clients));
            cmbClient.setValue(NO_CLIENT_OPTION);
            cmbArchive.setItems(FXCollections.observableArrayList());
            cmbArchive.setDisable(true);


        }
        catch (SQLException e)
        {
            uiManager.error("Load error: " + e.getMessage());
        }

        cmbClient.valueProperty().addListener((obs, old, val) ->
        {
            sessionManager.clearSelectionAndPages(navigation, scanningService);
            explorerTreeManager.refreshArchivesForClient(cmbArchive, allArchives, val);
            showBoxes();
            updateUI();
        });

        cmbArchive.valueProperty().addListener((obs, old, val) ->
        {
            sessionManager.clearSelectionAndPages(navigation, scanningService);
            showBoxes();
            updateUI();
        });

        profileDropdownManager.setup(cmbClient, cmbSessionRotation);

        explorerTreeManager.refreshArchivesForClient(cmbArchive, allArchives, cmbClient.getValue());
    }

    private void setupTreeView()
    {
        explorerTreeManager.setupTreeInteractions(
                sessionManager::getLevel,
                sessionManager::isRunning,
                this::reorderPage,
                this::openFile,
                this::handleSelection,
                this::movePageToDocument
        );
    }


    private void setupShortcuts()
    {
        shortcutManager = new ShortcutManager(
                rootPane,
                this::onStartScanning,
                this::onStopScanning,
                this::onPrev,
                this::onNext,
                this::onRotateLeft,
                this::onRotateRight,
                this::onSplitDocument,
                () -> onDelete(new ActionEvent()),
                this::onOpenMetadata,
                this::onOpenSlideView,
                this::onOpenShortcutSettings
        );

        if (rootPane.getScene() != null)
        {
            shortcutManager.install(rootPane.getScene());
        }
        else
        {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) ->
            {
                if (newScene != null)
                {
                    shortcutManager.install(newScene);
                }
            });
        }
    }

    private void movePageToDocument(Files page, Document targetDoc)
    {
        if (sessionManager.getSelectedBox() == null) return;

        splitter.movePageToDocument(
                sessionManager.getSelectedBox(),
                sessionManager.getScannedPages(),
                page,
                targetDoc,
                () -> {
                    explorerTreeManager.expandBoxPreservingState(sessionManager.getSelectedBox());
                    updateUI();
                }
        );
    }

    private void onTreeSelectionChanged(Object selected)
    {
        if (selected instanceof Boxes)
        {
            sessionManager.setLevel(Level.BOXES);
        }
        else if (selected instanceof Document)
        {
            sessionManager.setLevel(Level.DOCUMENTS);
        }
        else if (selected instanceof Files)
        {
            sessionManager.setLevel(Level.FILES);
        }
        updateUI();
    }

    @FXML
    private void onDelete(ActionEvent event)
    {
        Object selected = explorerTreeManager.getSelectedValue();
        if (selected == null)
        {
            uiManager.error("Nothing selected to delete.");
            return;
        }

        Level deleteLevel = explorerTreeManager.getLevelForSelected(selected);
        if ((deleteLevel == Level.DOCUMENTS || deleteLevel == Level.FILES)
                && sessionManager.getSelectedBox() == null)
        {
            Boxes ownerBox = explorerTreeManager.getOwnerBoxForSelected();
            if (ownerBox != null)
            {
                try
                {
                    sessionManager.openBox(ownerBox, archiveService, scanningService, explorerTreeManager);
                }
                catch (SQLException e)
                {
                    uiManager.error("Failed to open box.");
                    return;
                }
            }
        }

        deleteManager.delete(deleteLevel, selected, sessionManager.getSelectedBox(),
                sessionManager.getScannedPages(), this::refreshAfterDelete);
        sessionManager.syncSelectedBoxFromSession(scanningService);
        updateUI();
    }

    private void refreshAfterDelete()
    {
        deleteManager.refreshAfterDelete(sessionManager, explorerTreeManager, this::showBoxes);
    }

    @FXML private void onPrev()        { navigation.prev(sessionManager.getScannedPages());  updateUI(); }
    @FXML private void onNext()        { navigation.next(sessionManager.getScannedPages());  updateUI(); }
    @FXML private void onRotateLeft()  { pageViewerManager.rotateLeft(sessionManager.getScannedPages(), navigation); }
    @FXML private void onRotateRight() { pageViewerManager.rotateRight(sessionManager.getScannedPages(), navigation); }

    @FXML
    private void onStartScanning()
    {
        if (sessionManager.getSelectedBox() == null)
        {
            uiManager.error("Open a box first.");
            return;
        }

        Profile selectedProfile = cmbProfile.getValue();
        if (selectedProfile == null)
        {
            uiManager.error("Select a profile first.");
            return;
        }


        try {
            sessionManager.prepareStartScanning(scanningService, archiveService);
        } catch (SQLException e) {
            uiManager.error("Could not clear box before scanning" + e.getMessage());
            return;
        }

        updateUI();

        polling.start(newPages -> Platform.runLater(() ->
        {
            if (!sessionManager.isRunning()) return;

            Integer sessionRotation = cmbSessionRotation.getValue();
            if (sessionRotation != null && sessionRotation != 0)
            {
                for (Files page : newPages)
                {
                    page.setRotationDegrees(sessionRotation);
                }
            }

            sessionManager.appendNewPages(newPages, navigation, scanningService, explorerTreeManager);
            uiManager.info("Page received.");
            updateUI();
        }));

        audit.log("Scan started", "Started scanning box: " + sessionManager.getSelectedBox().getBoxId()
                + " | Profile: " + selectedProfile.getName()
                + " | Session rotation: " + cmbSessionRotation.getValue() + "\u00b0");
    }

    @FXML
    private void onStopScanning()
    {
        sessionManager.stopScanning(scanningService);
        polling.stop();

        if (sessionManager.getSelectedBox() == null)
        {
            updateUI();
            return;
        }

        try
        {
            archiveService.saveBoxSnapshot(sessionManager.getSelectedBox());

            Boxes reloaded = archiveService.loadBoxContent(sessionManager.getSelectedBox().getBoxId());
            sessionManager.getSelectedBox().replaceContent(reloaded.getPages(), reloaded.getDocuments());
            sessionManager.getScannedPages().clear();
            sessionManager.getScannedPages().addAll(reloaded.getPages());
            scanningService.loadSessionPages(sessionManager.getScannedPages());
            explorerTreeManager.expandBox(sessionManager.getSelectedBox());

            audit.log("Scan stopped", "Stopped scanning box: " + sessionManager.getSelectedBox().getBoxId());
        }
        catch (SQLException e)
        {
            uiManager.error("Save failed: " + e.getMessage());
        }

        updateUI();
    }

    @FXML private void onExportSinglePage() { if (sessionManager.getSelectedBox() != null) exporter.exportSingle(sessionManager.getSelectedBox()); }
    @FXML private void onExportMultiPage()  { if (sessionManager.getSelectedBox() != null) exporter.exportMulti(sessionManager.getSelectedBox()); }

    @FXML
    private void onSplitDocument()
    {
        if (sessionManager.getSelectedBox() == null)
        {
            uiManager.error("Open a box first.");
            return;
        }

        splitter.split(
                sessionManager.getSelectedBox(),
                sessionManager.getScannedPages(),
                navigation.getIndex(),
                () ->
                {
                    showBoxes();
                    explorerTreeManager.expandBox(sessionManager.getSelectedBox());
                    sessionManager.resetToDocumentsLevel();
                    updateUI();
                }
        );
    }

    @FXML
    private void onOpenMetadata()
    {
        if (sessionManager.getSelectedBox() == null)
        {
            uiManager.error("Open a box first.");
            return;
        }
        uiManager.openMetadataDialog(sessionManager.getSelectedBox().getBoxId());
    }

    @FXML
    private void onOpenSlideView()
    {
        if (sessionManager.getSelectedBox() == null)
        {
            uiManager.error("Open a box first.");
            return;
        }
        uiManager.openSlideView(sessionManager.getSelectedBox().getPages(), navigation.getIndex());
    }

    @FXML
    private void onOpenShortcutSettings()
    {
        uiManager.openShortcutSettingsDialog();
    }

    /**
     * Marks the currently selected document (or the document that contains the
     * currently selected file) as Waiting for QA and persists the status change.
     */
    @FXML
    private void onMarkWaitingForQA()
    {
        Document docToMark = resolveCurrentDocument();

        if (docToMark == null)
        {
            uiManager.error("Select a document to mark for QA.");
            return;
        }

        if (docToMark.getId() <= 0)
        {
            uiManager.error("Stop scanning and save the session before marking for QA.");
            return;
        }

        if (docToMark.getStatus() == DocumentStatus.WAITING_FOR_QA)
        {
            uiManager.info("Document " + docToMark.getDocumentNumber() + " is already marked as Waiting for QA.");
            return;
        }

        try
        {
            archiveService.updateDocumentStatus(docToMark.getId(), DocumentStatus.WAITING_FOR_QA);
            docToMark.setStatus(DocumentStatus.WAITING_FOR_QA);

            if (sessionManager.getSelectedBox() != null)
            {
                explorerTreeManager.expandBox(sessionManager.getSelectedBox());
            }

            uiManager.success("Document " + docToMark.getDocumentNumber() + " marked as Waiting for QA.");
            audit.log("Box marked for QA", "Document " + docToMark.getDocumentNumber()
                    + " in box " + sessionManager.getSelectedBox().getBoxId() + " marked as Waiting for QA.");
        }
        catch (SQLException e)
        {
            uiManager.error("Could not update document status: " + e.getMessage());
        }
    }

    /**
     * Returns the document that is currently in context: either the directly
     * selected document or the parent document of the selected file.
     */
    private Document resolveCurrentDocument()
    {
        Object selected = explorerTreeManager.getSelectedValue();
        if (selected instanceof Document d)
        {
            return d;
        }
        return sessionManager.getSelectedDocument();
    }

    private void handleSelection()
    {
        Object selected = explorerTreeManager.getSelectedValue();
        if (selected == null) return;

        if (selected instanceof Boxes box)
        {
            openBox(box);
        }
        else if (selected instanceof Document document)
        {
            openDocument(document);
        }
        else if (selected instanceof Files file)
        {
            openFile(file);
        }
    }

    private void openBox(Boxes box)
    {
        try
        {
            sessionManager.openBox(box, archiveService, scanningService, explorerTreeManager);
            lblExplorerPath.setText("Boxes / " + sessionManager.getSelectedBox().getBoxId());
            updateUI();
        }
        catch (SQLException e)
        {
            uiManager.error("Failed to open box.");
        }
    }

    private void openDocument(Document document)
    {
        if (sessionManager.openDocument(document, explorerTreeManager))
        {
            lblExplorerPath.setText("Boxes / " + sessionManager.getSelectedBox().getBoxId()
                    + " / " + sessionManager.getSelectedDocument());
            updateUI();
        }
    }

    private void openFile(Files file)
    {
        if (navigation.openFile(sessionManager.getScannedPages(), file, explorerTreeManager))
        {
            sessionManager.setLevel(Level.FILES);
            updateUI();
        }
    }

    private void reorderPage(Files source, Files target)
    {
        int from = explorerTreeManager.findPageIndex(sessionManager.getScannedPages(), source);
        int to   = explorerTreeManager.findPageIndex(sessionManager.getScannedPages(), target);
        if (from < 0 || to < 0) return;

        scanningService.movePage(from, to);
        sessionManager.getScannedPages().clear();
        sessionManager.getScannedPages().addAll(scanningService.getAllPages());
        sessionManager.syncSelectedBoxFromSession(scanningService);

        if (sessionManager.getSelectedBox() != null)
        {
            try
            {
                archiveService.updatePageOrder(sessionManager.getSelectedBox().getBoxId(),
                        sessionManager.getScannedPages());
            }
            catch (SQLException e)
            {
                uiManager.error("Could not update page order: " + e.getMessage());
            }
            explorerTreeManager.expandBox(sessionManager.getSelectedBox());
            if (sessionManager.getSelectedDocument() != null)
            {
                explorerTreeManager.expandDocument(sessionManager.getSelectedBox(),
                        sessionManager.getSelectedDocument());
            }
        }
        updateUI();
    }

    private void showBoxes()
    {
        sessionManager.setLevel(Level.BOXES);
        lblExplorerPath.setText("Boxes");
        explorerTreeManager.showFilteredBoxes(allBoxes, cmbClient.getValue(), cmbArchive.getValue());
    }

    @FXML
    private void onBack()
    {
        if (sessionManager.getLevel() == Level.FILES)
        {
            sessionManager.resetToDocumentsLevel();
            lblExplorerPath.setText(sessionManager.getSelectedBox() == null
                    ? "Boxes"
                    : "Boxes / " + sessionManager.getSelectedBox().getBoxId());
            updateUI();
            return;
        }

        if (sessionManager.getLevel() == Level.DOCUMENTS)
        {
            sessionManager.resetToBoxesLevel(navigation, scanningService);
            showBoxes();
            updateUI();
        }
    }

    private void updateUI()
    {
        boolean hasPage = navigation.getIndex() >= 0 && !sessionManager.getScannedPages().isEmpty();
        Object selected = explorerTreeManager == null ? null : explorerTreeManager.getSelectedValue();

        uiManager.applyState(
                new UiManager.UiState(
                        sessionManager.isRunning(),
                        hasPage,
                        sessionManager.getSelectedBox() != null,
                        selected != null,
                        sessionManager.getSelectedBox() != null
                                && !sessionManager.getSelectedBox().getDocuments().isEmpty(),
                        navigation.getIndex(),
                        sessionManager.getScannedPages().size()
                ),
                btnPrev, btnNext, btnRotateLeft, btnRotateRight, btnSplit,
                btnStart, btnStop, btnDelete, btnBack, btnExportSingle, btnExportMulti
        );

        btnMetadata.setDisable(sessionManager.getSelectedBox() == null);

        boolean canMarkQA = !sessionManager.isRunning() && resolveCurrentDocument() != null;
        btnMarkQA.setDisable(!canMarkQA);

        uiManager.updateScanningSummary(
                lblTotalScans,
                lblCurrentBox,
                sessionManager.getScannedPages().size(),
                sessionManager.getSelectedBox()
        );
    }

    @FXML
    private void onCreateBox()
    {
        try
        {
            Boxes createdBox = explorerTreeManager.createBox(
                    txtBoxId.getText(),
                    cmbClient.getValue(),
                    cmbArchive.getValue(),
                    archiveService,
                    allBoxes
            );
            txtBoxId.clear();
            showBoxes();
            uiManager.success("Created box " + createdBox.getBoxId());
        }
        catch (IllegalArgumentException e)
        {
            uiManager.error(e.getMessage());
        }
        catch (Exception e)
        {
            uiManager.error("Could not create box: " + e.getMessage());
        }
    }
}