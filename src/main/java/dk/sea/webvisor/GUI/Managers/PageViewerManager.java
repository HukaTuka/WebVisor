package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;

// Java Imports
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
                BufferedImage toDisplay = applyRotation(page.getImage(), page.getRotationDegrees());
                view.setImage(SwingFXUtils.toFXImage(toDisplay, null));
            }
        } catch (SQLException e) {
            uiManager.error("Could not load image");
        }
    }

    private BufferedImage applyRotation(BufferedImage source, int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        if (normalized == 0) {
            return source;
        }

        double radians = Math.toRadians(normalized);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth  = (int) Math.floor(source.getWidth() * cos + source.getHeight() * sin);
        int newHeight = (int) Math.floor(source.getHeight() * cos + source.getWidth() * sin);

        int imageType = source.getType() == BufferedImage.TYPE_CUSTOM
                ? BufferedImage.TYPE_INT_ARGB
                : source.getType();

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, imageType);
        Graphics2D g2d = rotated.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AffineTransform tx = new AffineTransform();
            tx.translate(newWidth / 2.0, newHeight / 2.0);
            tx.rotate(radians);
            tx.translate(-source.getWidth() / 2.0, -source.getHeight() / 2.0);
            g2d.drawImage(source, tx, null);
        } finally {
            g2d.dispose();
        }

        return rotated;
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

