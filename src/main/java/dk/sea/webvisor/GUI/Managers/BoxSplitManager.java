package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.*;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.ScanningService;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import java.sql.SQLException;
import java.util.List;

public class BoxSplitManager {

    private final ArchiveService archiveService;
    private final ScanningService scanningService;
    private final UiManager uiManager;
    private final AuditService audit;

    public BoxSplitManager(ArchiveService archiveService,
                           ScanningService scanningService,
                           UiManager uiManager,
                           AuditService audit)
    {
        this.archiveService = archiveService;
        this.scanningService = scanningService;
        this.uiManager = uiManager;
        this.audit = audit;
    }

    public void split(Boxes box, List<Files> pages, int index, Runnable after) {
        if (index < 0 || index >= pages.size() - 1) {
            uiManager.error("Invalid split position.");
            return;
        }

        try {
            archiveService.splitDocumentAt(box, index);
            after.run();
            audit.log("Pages were split", "Document split at page " + index);

        } catch (SQLException e) {
            uiManager.error("Split failed: " + e.getMessage());
        }
    }
}

