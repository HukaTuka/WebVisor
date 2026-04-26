package dk.sea.webvisor.GUI.Controllers;

// Project imports
import dk.sea.webvisor.BE.ScannedPage;
import dk.sea.webvisor.BLL.ScanningService;

// JavaFX imports
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * GUI controller for the scanning session screen.
 *
 * <h3>Threading model</h3>
 * Polling runs on a dedicated daemon thread ({@code pollingThread}).
 * All UI updates happen via {@link Platform#runLater(Runnable)}.
 * The controller uses a volatile flag ({@code running}) to signal the
 * thread to stop cleanly without interrupting blocking I/O mid-flight.
 */
public class ScanningController
{
    @FXML private Button    btnStart;
    @FXML private Button    btnStop;
    @FXML private Button    btnRotateLeft;
    @FXML private Button    btnRotateRight;
    @FXML private Button    btnPrev;
    @FXML private Button    btnNext;
    @FXML private Label     lblPageInfo;
    @FXML private Label     lblStatus;
    @FXML private ImageView imgPage;
    @FXML private ListView<ScannedPage> lstPages;


    private final ScanningService           scanningService  = new ScanningService();
    private final ObservableList<ScannedPage> pageItems      = FXCollections.observableArrayList();

    private volatile boolean running         = false;
    private Thread           pollingThread   = null;
    private int              currentIndex    = -1;

    /** Interval between API polls while a session is active (milliseconds). */
    private static final long POLL_INTERVAL_MS = 3_000;


    @FXML
    private void initialize()
    {
        lstPages.setItems(pageItems);

        // Selecting a page in the list navigates to it
        lstPages.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldVal, newVal) ->
                {
                    int idx = newVal.intValue();
                    if (idx >= 0)
                    {
                        navigateTo(idx);
                    }
                }
        );

        updateButtonState();
    }


    @FXML
    private void onStartScanning()
    {
        if (running)
        {
            return;
        }

        // Ask the operator whether to keep or clear any existing pages
        if (!pageItems.isEmpty())
        {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Start a new session? This will clear all current pages.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
            {
                return;
            }
        }

        scanningService.clearSession();
        pageItems.clear();
        currentIndex = -1;
        imgPage.setImage(null);
        showStatus("Scanning started — polling API every " + POLL_INTERVAL_MS / 1000 + " s…", "status-info");

        running = true;
        updateButtonState();

        pollingThread = new Thread(this::pollLoop, "scanning-poll-thread");
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    @FXML
    private void onStopScanning()
    {
        running = false;
        updateButtonState();
        showStatus("Scanning stopped. " + pageItems.size() + " page(s) collected.", "status-info");
    }


    @FXML
    private void onPrev()
    {
        if (currentIndex > 0)
        {
            navigateTo(currentIndex - 1);
        }
    }

    @FXML
    private void onNext()
    {
        if (currentIndex < pageItems.size() - 1)
        {
            navigateTo(currentIndex + 1);
        }
    }


    @FXML
    private void onRotateLeft()
    {
        applyRotation(false);
    }

    @FXML
    private void onRotateRight()
    {
        applyRotation(true);
    }

    private void applyRotation(boolean clockwise)
    {
        if (currentIndex < 0 || currentIndex >= pageItems.size())
        {
            return;
        }

        ScannedPage page = pageItems.get(currentIndex);
        if (clockwise)
        {
            page.rotateRight();
        }
        else
        {
            page.rotateLeft();
        }

        displayPage(page);
    }

    /**
     * Blocking polling loop – runs entirely on {@code pollingThread}.
     * Each iteration fetches one batch from the API, then sleeps.
     * UI updates are dispatched via {@link Platform#runLater}.
     */
    private void pollLoop()
    {
        while (running)
        {
            try
            {
                List<ScannedPage> newPages = scanningService.fetchAndAppendNext();

                Platform.runLater(() -> handleNewPages(newPages));

                Thread.sleep(POLL_INTERVAL_MS);
            }
            catch (IOException e)
            {
                Platform.runLater(() ->
                        showStatus("API error: " + e.getMessage() + " — retrying…", "status-error"));

                try { Thread.sleep(POLL_INTERVAL_MS); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Called on the JavaFX Application Thread after each successful poll. */
    private void handleNewPages(List<ScannedPage> newPages)
    {
        if (newPages.isEmpty())
        {
            return;
        }

        boolean barcodeFound = false;

        for (ScannedPage page : newPages)
        {
            pageItems.add(page);
            if (page.isBarcode())
            {
                barcodeFound = true;
            }
        }

        // Auto-navigate to the latest page
        navigateTo(pageItems.size() - 1);

        if (barcodeFound)
        {
            showStatus("⚠ Barcode detected — document split at page " +
                    newPages.stream().filter(dk.sea.webvisor.BE.ScannedPage::isBarcode)
                            .mapToInt(ScannedPage::getPageNumber).min().orElse(-1),
                    "status-error");
        }
        else
        {
            showStatus("ScannedPage " + newPages.get(0).getPageNumber() + " received.", "status-info");
        }
    }

    private void navigateTo(int index)
    {
        if (index < 0 || index >= pageItems.size())
        {
            return;
        }

        currentIndex = index;
        lstPages.getSelectionModel().select(index);
        lstPages.scrollTo(index);
        displayPage(pageItems.get(index));
        updatePageLabel();
        updateNavigationButtons();
    }

    /**
     * Converts the {@link BufferedImage} to a JavaFX {@link Image}, applies
     * any operator-requested rotation, and renders it in the {@link ImageView}.
     */
    private void displayPage(ScannedPage page)
    {
        BufferedImage raw     = page.getImage();
        BufferedImage rotated = applyAwtRotation(raw, page.getRotationDegrees());
        Image         fxImage = SwingFXUtils.toFXImage(rotated, null);

        imgPage.setImage(fxImage);

        // Fit the image inside the ImageView while preserving aspect ratio
        imgPage.setPreserveRatio(true);
        imgPage.setFitWidth(imgPage.getFitWidth());  // trigger layout pass

        // Tint barcode pages with a visual indicator via CSS rotation trick —
        // we use an ImageView opacity instead of a separate overlay node to
        // keep the FXML minimal.
        imgPage.setOpacity(page.isBarcode() ? 0.55 : 1.0);
        imgPage.setRotate(0); // rotation already baked into the BufferedImage
    }

    /**
     * Rotates a {@link BufferedImage} by {@code degrees} (0/90/180/270) using
     * AWT's {@link AffineTransformOp}, which produces a correctly-sized result.
     */
    private BufferedImage applyAwtRotation(BufferedImage src, int degrees)
    {
        if (degrees == 0)
        {
            return src;
        }

        double radians = Math.toRadians(degrees);
        double sin     = Math.abs(Math.sin(radians));
        double cos     = Math.abs(Math.cos(radians));

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

    private void updatePageLabel()
    {
        int total = pageItems.size();
        int shown = total == 0 ? 0 : currentIndex + 1;
        lblPageInfo.setText("ScannedPage " + shown + " / " + total);
    }

    private void updateNavigationButtons()
    {
        btnPrev.setDisable(currentIndex <= 0);
        btnNext.setDisable(currentIndex >= pageItems.size() - 1);
    }

    private void updateButtonState()
    {
        btnStart.setDisable(running);
        btnStop.setDisable(!running);
        btnRotateLeft.setDisable(currentIndex < 0);
        btnRotateRight.setDisable(currentIndex < 0);
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }
}
