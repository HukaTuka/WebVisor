package dk.sea.webvisor.BLL;

import dk.sea.webvisor.BE.ScannedPage;
import dk.sea.webvisor.BLL.Util.BarcodeDetector;
import dk.sea.webvisor.DAL.API.TiffApiClient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BLL service that owns the lifecycle of a scanning session.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Delegates HTTP calls to {@link TiffApiClient}.</li>
 *   <li>Runs barcode detection on every returned image.</li>
 *   <li>Maintains the ordered list of {@link ScannedPage} objects for the
 *       current session.</li>
 *   <li>Exposes a simple {@link #fetchAndAppendNext()} method that the GUI
 *       controller calls from a background thread.</li>
 * </ul>
 *
 * <p><strong>Thread-safety</strong>: {@code pages} is wrapped in a
 * synchronised list so the controller may safely read it from the JavaFX
 * Application Thread while the polling task writes from a background thread.
 */
public class ScanningService
{
    private final TiffApiClient      apiClient;
    private final List<ScannedPage>  pages;
    private final AtomicInteger      pageCounter;

    public ScanningService()
    {
        this.apiClient    = new TiffApiClient();
        this.pages        = Collections.synchronizedList(new ArrayList<>());
        this.pageCounter  = new AtomicInteger(0);
    }

    /**
     * Calls the TIFF API once, analyses each returned image for barcodes,
     * wraps them in {@link ScannedPage} objects, appends them to the session,
     * and returns only the newly added pages.
     *
     * <p>Call this from a background thread – it performs a blocking HTTP
     * request.
     *
     * @return newly added pages (never {@code null}, may be empty)
     * @throws IOException if the API call or ZIP parsing fails
     */
    public List<ScannedPage> fetchAndAppendNext() throws IOException
    {
        List<BufferedImage> images   = apiClient.fetchRandomPage();
        List<ScannedPage>   newPages = new ArrayList<>();

        for (BufferedImage image : images)
        {
            boolean     barcode = BarcodeDetector.isBarcode(image);
            ScannedPage page    = new ScannedPage(pageCounter.incrementAndGet(), image, barcode);
            pages.add(page);
            newPages.add(page);
        }

        return newPages;
    }

    /**
     * Returns an unmodifiable snapshot of all pages collected in this session.
     */
    public List<ScannedPage> getAllPages()
    {
        synchronized (pages)
        {
            return List.copyOf(pages);
        }
    }

    /**
     * Resets the session: clears all pages and resets the page counter.
     * Call this before starting a fresh scan.
     */
    public void clearSession()
    {
        synchronized (pages)
        {
            pages.clear();
        }
        pageCounter.set(0);
    }

    /** Returns the total number of pages collected so far. */
    public int getPageCount()
    {
        return pageCounter.get();
    }
}
