package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;

// Java Imports
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import java.sql.SQLException;
import java.util.List;

public class PageViewerManager {

    private final ArchiveService archiveService;
    private final UiManager uiManager;

    public PageViewerManager(ArchiveService archiveService, UiManager uiManager) {
        this.archiveService = archiveService;
        this.uiManager = uiManager;
    }

    public void show(Files page, ImageView view) {
        try {
            if (page.getImage() == null && page.getId() > 0) {
                page.setImage(archiveService.loadFileImage(page.getId()));
            }

            if (page.getImage() != null) {
                view.setImage(SwingFXUtils.toFXImage(page.getImage(), null));
            }
        } catch (SQLException e) {
            uiManager.error("Could not load image: " + e.getMessage());
        }
    }

    public void rotateLeft(List<Files> pages, PageNavigationManager PageNavigationManager) {
        if (PageNavigationManager.getIndex() < 0) {
            return;
        }

        Files page = pages.get(PageNavigationManager.getIndex());
        page.rotateLeft();
        PageNavigationManager.refresh(pages);
    }

    public void rotateRight(List<Files> pages, PageNavigationManager PageNavigationManager) {
        if (PageNavigationManager.getIndex() < 0) {
            return;
        }

        Files page = pages.get(PageNavigationManager.getIndex());
        page.rotateRight();
        PageNavigationManager.refresh(pages);
    }
}

