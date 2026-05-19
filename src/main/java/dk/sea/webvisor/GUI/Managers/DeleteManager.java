package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.*;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.ScanningService;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.GUI.Controllers.ScanningController;

// Java Imports
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeleteManager {

    private final ArchiveService archiveService;
    private final ScanningService scanningService;
    private final UiManager uiManager;
    private final AuditService audit;

    public DeleteManager(ArchiveService archiveService,
                         ScanningService scanningService,
                         UiManager uiManager,
                         AuditService audit) {
        this.archiveService = archiveService;
        this.scanningService = scanningService;
        this.uiManager = uiManager;
        this.audit = audit;
    }

    /**
     * Main delete entry point.
     *
     * @param level     Which level user is on (BOXES, DOCUMENTS, FILES)
     * @param selected  Selected item in ListView (Box, Document, File)
     * @param selectedBox Current box (can be null)
     * @param scannedPages List of current pages
     * @param onRefresh Callback to update UI after delete
     */
    public void delete(ScanningController.Level level,
                       Object selected,
                       Boxes selectedBox,
                       List<Files> scannedPages,
                       Runnable onRefresh) {

        if (selected == null) {
            uiManager.error("Nothing selected to delete.");
            return;
        }

        try {
            switch (level) {
                case BOXES -> deleteBox((Boxes) selected, onRefresh);
                case DOCUMENTS -> deleteDocument((Document) selected, selectedBox, scannedPages, onRefresh);
                case FILES -> deletePage((Files) selected, selectedBox, scannedPages, onRefresh);
            }
        } catch (IllegalArgumentException e) {
            uiManager.error(e.getMessage());
        } catch (SQLException e) {
            uiManager.error("Database error while deleting: " + e.getMessage());
        }
    }

    private void deleteBox(Boxes box, Runnable onRefresh) throws SQLException {
        archiveService.deleteBox(box.getBoxId());
        uiManager.success("Box deleted: " + box.getBoxId());
        audit.log("Box deleted", "Deleted box: " + box.getBoxId());
        onRefresh.run();
    }

    private void deleteDocument(Document doc,
                                Boxes box,
                                List<Files> scannedPages,
                                Runnable onRefresh) throws SQLException {
        if (box == null) {
            throw new IllegalArgumentException("Open a box before deleting a document.");
        }

        // Delete all pages belonging to this document
        for (Files p : doc.getPages()) {
            if (p.getId() > 0)
                archiveService.deleteFileById(p.getId());
        }

        archiveService.deleteDocumentByNumber(box.getBoxId(), doc.getDocumentNumber());

        // Rebuild pages list (remove doc pages)
        List<Files> remaining = new ArrayList<>();
        for (Files p : scannedPages)
            if (!doc.getPages().contains(p))
                remaining.add(p);

        scanningService.loadSessionPages(remaining);
        scannedPages.clear();
        scannedPages.addAll(remaining);

        uiManager.success("Document deleted.");
        audit.log("Document deleted", "Deleted document " + doc);
        onRefresh.run();
    }

    private void deletePage(Files page,
                            Boxes box,
                            List<Files> scannedPages,
                            Runnable onRefresh) throws SQLException {
        if (box == null) {
            throw new IllegalArgumentException("Open a box before deleting a page.");
        }

        // Delete from DB
        if (page.getId() > 0)
            archiveService.deleteFileById(page.getId());

        // Delete from scanning session
        int index = scannedPages.indexOf(page);
        scanningService.deletePageAt(index);

        // Update local list
        scannedPages.clear();
        scannedPages.addAll(scanningService.getAllPages());

        // Persist page order
        archiveService.updatePageOrder(box.getBoxId(), scannedPages);

        uiManager.success("Page deleted: " + page.getReferenceId());
        audit.log("Page delted", "Deleted page: " + page.getReferenceId());

        onRefresh.run();
    }

    public void refreshAfterDelete(ScanningSessionManager sessionManager,
                                   ExplorerTreeManager explorerTreeManager,
                                   Runnable showBoxes) {
        showBoxes.run();
        if (sessionManager.getSelectedBox() != null) {
            explorerTreeManager.expandBox(sessionManager.getSelectedBox());
            if (sessionManager.getSelectedDocument() != null) {
                explorerTreeManager.expandDocument(sessionManager.getSelectedDocument());
            }
        }
    }
}