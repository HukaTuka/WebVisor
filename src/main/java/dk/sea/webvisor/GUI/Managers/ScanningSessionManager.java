package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.ScanningService;
import dk.sea.webvisor.GUI.Controllers.ScanningController;

// Java Imports
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ScanningSessionManager {
    private ScanningController.Level level = ScanningController.Level.BOXES;
    private final List<Files> scannedPages = new ArrayList<>();
    private Boxes selectedBox;
    private Document selectedDocument;
    private boolean running;

    public ScanningController.Level getLevel() {
        return level;
    }

    public void setLevel(ScanningController.Level level) {
        this.level = level;
    }

    public List<Files> getScannedPages() {
        return scannedPages;
    }

    public Boxes getSelectedBox() {
        return selectedBox;
    }

    public Document getSelectedDocument() {
        return selectedDocument;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void clearSelectionAndPages(PageNavigationManager navigation, ScanningService scanningService) {
        selectedBox = null;
        selectedDocument = null;
        scannedPages.clear();
        scanningService.loadSessionPages(scannedPages);
        navigation.refresh(scannedPages);
    }

    public void syncSelectedBoxFromSession(ScanningService scanningService) {
        if (selectedBox == null) return;
        selectedBox.replaceContent(scannedPages, scanningService.getDocuments());
    }

    public void openBox(Boxes box,
                        ArchiveService archiveService,
                        ScanningService scanningService,
                        ExplorerTreeManager explorerTreeManager) throws SQLException {
        Boxes hydrated = archiveService.loadBoxContent(box.getBoxId());
        box.replaceContent(hydrated.getPages(), hydrated.getDocuments());

        selectedBox = box;
        selectedDocument = null;
        scannedPages.clear();
        scannedPages.addAll(selectedBox.getPages());
        scanningService.loadSessionPages(scannedPages);
        explorerTreeManager.expandBox(selectedBox);
        level = ScanningController.Level.DOCUMENTS;
    }

    public boolean openDocument(Document document, ExplorerTreeManager explorerTreeManager) {
        if (selectedBox == null) return false;

        selectedDocument = explorerTreeManager.resolveSelectedDocument(selectedBox, document);
        if (selectedDocument == null) return false;

        explorerTreeManager.expandDocument(selectedBox, selectedDocument);
        level = ScanningController.Level.FILES;
        return true;
    }

    public void prepareStartScanning(ScanningService scanningService, ArchiveService archiveService) throws SQLException {
        running = true;

        if (selectedBox != null) {
            archiveService.deleteAllContentsForBox(selectedBox.getBoxId());
        }

        scannedPages.clear();
        scanningService.clearSession();
        selectedDocument = null;
    }

    public void appendNewPages(List<Files> newPages,
                               PageNavigationManager navigation,
                               ScanningService scanningService,
                               ExplorerTreeManager explorerTreeManager) {
        scannedPages.addAll(newPages);
        navigation.goTo(scannedPages, scannedPages.size() - 1);
        syncSelectedBoxFromSession(scanningService);
        if (selectedBox != null) {
            explorerTreeManager.expandBox(selectedBox);
        }
    }

    public void stopScanning(ScanningService scanningService) {
        running = false;
        syncSelectedBoxFromSession(scanningService);
    }

    public void resetToDocumentsLevel() {
        selectedDocument = null;
        level = ScanningController.Level.DOCUMENTS;
    }

    public void resetToBoxesLevel(PageNavigationManager navigation, ScanningService scanningService) {
        level = ScanningController.Level.BOXES;
        clearSelectionAndPages(navigation, scanningService);
    }
}

