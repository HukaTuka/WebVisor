package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BLL.ExportService;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.io.IOException;

public class ExportManager {

    private final ExportService exportService;
    private final UiManager uiManager;
    private final AuditService audit;

    public ExportManager(ExportService exportService, UiManager uiManager, AuditService audit) {
        this.exportService = exportService;
        this.uiManager = uiManager;
        this.audit = audit;
    }

    public void exportSingle(Boxes box) {
        File dir = choose();
        if (dir == null) return;

        Thread t = new Thread(() -> {
            try {
                int count = exportService.exportSinglePage(box.getDocuments(), dir, box.getBoxId());
                uiManager.success("Exported " + count + " pages.");
                audit.log("Export single", "Exported " + count);

            } catch (IOException e) {
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
                int count = exportService.exportMultiPage(box.getDocuments(), dir, box.getBoxId());
                uiManager.success("Exported " + count + " documents.");
                audit.log("Exported as multi page", "Exported " + count);

            } catch (IOException e) {
                uiManager.error("Export failed: " + e.getMessage());
            }
        });

        t.setDaemon(true);
        t.start();
    }

    private File choose() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose export folder");
        return chooser.showDialog(null);
    }
}

