package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Files;

// Java Imports
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import java.util.List;

public class PageNavigationManager {

    private final ImageView imageView;
    private final Label lblPageInfo;
    private final PageViewerManager pageViewerManager;

    private int index = -1;

    public PageNavigationManager(ImageView imageView, Label lblPageInfo, PageViewerManager pageViewerManager) {
        this.imageView = imageView;
        this.lblPageInfo = lblPageInfo;
        this.pageViewerManager = pageViewerManager;
    }

    public int getIndex() {
        return index;
    }

    public void goTo(List<Files> pages, int newIndex) {
        if (newIndex < 0 || newIndex >= pages.size()) return;

        index = newIndex;
        Files page = pages.get(newIndex);

        pageViewerManager.show(page, imageView);
        lblPageInfo.setText("Page " + (newIndex + 1) + " / " + pages.size());
    }

    public void prev(List<Files> pages) {
        goTo(pages, index - 1);
    }

    public void next(List<Files> pages) {
        goTo(pages, index + 1);
    }

    public void refresh(List<Files> pages) {
        if (pages.isEmpty()) {
            imageView.setImage(null);
            lblPageInfo.setText("Page 0 / 0");
            index = -1;
            return;
        }

        if (index >= pages.size()) index = pages.size() - 1;
        goTo(pages, index);
    }

    public boolean openFile(List<Files> pages, Files file, ExplorerTreeManager explorerTreeManager) {
        int fileIndex = explorerTreeManager.findPageIndex(pages, file);
        if (fileIndex < 0) {
            return false;
        }

        goTo(pages, fileIndex);
        return true;
    }
}

