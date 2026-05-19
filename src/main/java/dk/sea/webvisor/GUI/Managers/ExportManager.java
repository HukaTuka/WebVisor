package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.ExportService;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ExportManager {

    private final ExportService exportService;
    private final ArchiveService archiveService;
    private final UiManager uiManager;
    private final AuditService audit;

    public ExportManager(ExportService exportService, ArchiveService archiveService, UiManager uiManager, AuditService audit) {
        this.exportService = exportService;
        this.archiveService = archiveService;
        this.uiManager = uiManager;
        this.audit = audit;
    }

    public void exportSingle(Boxes box) {
        File dir = choose();
        if (dir == null) return;

        Thread t = new Thread(() -> {
            try {
                hydrateDocumentImages(box.getDocuments());
                int count = exportService.exportSinglePage(box.getDocuments(), dir, box.getBoxId());
                uiManager.success("Exported " + count + " pages.");
                audit.log("Exported as single page", "Exported " + count + " pages from box: " + box.getBoxId());

            } catch (IOException | SQLException e) {
                uiManager.error("Export failed: " + e.getMessage());
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public void exportMulti(Boxes box) {
        File dir = choose();
        if (dir == null) return;

        Thread t = new Thread(() -> {
            try {
                hydrateDocumentImages(box.getDocuments());
                int count = exportService.exportMultiPage(box.getDocuments(), dir, box.getBoxId());
                uiManager.success("Exported " + count + " documents.");
                audit.log("Exported as multi page", "Exported " + count + " documents from box: " + box.getBoxId());

            } catch (IOException | SQLException e) {
                uiManager.error("Export failed: " + e.getMessage());
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private void hydrateDocumentImages(List<Document> documents) throws SQLException
    {
        for (Document document : documents)
        {
            for (Files page : document.getPages())
            {
                if (page.getImage() == null && page.getId() > 0)
                {
                    page.setImage(archiveService.loadFileImage(page.getId()));
                }
            }
        }
    }

    private File choose() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose export folder");
        return chooser.showDialog(null);
    }
}