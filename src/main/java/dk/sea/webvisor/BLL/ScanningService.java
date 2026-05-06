package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.Util.BarcodeDetector;
import dk.sea.webvisor.DAL.API.TiffApiClient;

// Java Imports
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
 *   <li>Maintains the ordered list of {@link Files} objects for the
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
    private final List<Files>        pages;
    private final AtomicInteger      pageCounter;
    // document tracking
    private final List<Document> documents = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger documentCounter = new AtomicInteger(0);
    private boolean startNewDocumentOnNextPage = false;

    public ScanningService()
    {
        this.apiClient    = new TiffApiClient();
        this.pages        = Collections.synchronizedList(new ArrayList<>());
        this.pageCounter  = new AtomicInteger(0);
    }

    /**
     * Calls the TIFF API once, analyses each returned image for barcodes,
     * wraps them in {@link Files} objects, appends them to the session,
     * and returns only the newly added pages.
     *
     * <p>Call this from a background thread – it performs a blocking HTTP
     * request.
     *
     * @return newly added pages (never {@code null}, may be empty)
     * @throws IOException if the API call or ZIP parsing fails
     */
    public List<Files> fetchAndAppendNext() throws IOException {
        List<BufferedImage> images = apiClient.fetchRandomPage();
        List<Files> newPages = new ArrayList<>();

        for (BufferedImage image : images) {
            boolean barcode = BarcodeDetector.isBarcode(image);
            Files page = new Files(pageCounter.incrementAndGet(), image, barcode);
            pages.add(page);
            newPages.add(page);

            if (documents.isEmpty()) {
                documents.add(new Document(documentCounter.incrementAndGet()));
            }

            // If previous page was a barcode, this page starts a new document
            if (startNewDocumentOnNextPage) {
                documents.add(new Document(documentCounter.incrementAndGet()));
                startNewDocumentOnNextPage = false;
            }

            // Always include the current page in the active document
            documents.get(documents.size() - 1).addPage(page);

            // Barcode signals a document split  start a fresh document
            if (barcode) {
                startNewDocumentOnNextPage = true;
            }
        }

        return newPages;
    }

    /**
     * Returns an unmodifiable snapshot of all pages collected in this session.
     */
    public List<Files> getAllPages()
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
    public void clearSession() {
        synchronized (pages) { pages.clear(); }
        synchronized (documents) { documents.clear(); }
        pageCounter.set(0);
        documentCounter.set(0);
        startNewDocumentOnNextPage = false;
    }

    public List<Document> getDocuments() {
        synchronized (documents) {
            return List.copyOf(documents);
        }
    }

    /**
     Deletes one page from the current session and rebuilds document grouping.

     @param pageIndex index in current page order
     @return true if deleted, false if index was invalid
     */
    public boolean deletePageAt(int pageIndex)
    {
        synchronized (pages)
        {
            if (pageIndex < 0 || pageIndex >= pages.size())
            {
                return false;
            }

            pages.remove(pageIndex);
            rebuildDocumentsFromPages();
            return true;
        }
    }

    private void rebuildDocumentsFromPages()
    {
        List<Document> rebuilt = new ArrayList<>();
        Document current = null;

        for (Files page : pages)
        {
            if (current == null)
            {
                current = new Document(rebuilt.size() + 1);
                rebuilt.add(current);
            }

            current.addPage(page);

            // Barcode closes the current document; next page starts a new one.
            if (page.isBarcode())
            {
                current = null;
            }
        }

        synchronized (documents)
        {
            documents.clear();
            documents.addAll(rebuilt);
        }

        documentCounter.set(rebuilt.size());
        startNewDocumentOnNextPage = !pages.isEmpty() && pages.get(pages.size() - 1).isBarcode();
    }

    /** Returns the total number of pages collected so far. */
    public int getPageCount()
    {
        return pageCounter.get();
    }

    /**
     * Inserts a manual document split after the page at the given index.
     * All pages from 0..splitIndex go into the current document,
     * pages from splitIndex+1 onward start a new document.
     *
     * @param pageIndex the index of the last page in the current document
     * @return true if the split was applied, false if the index was invalid
     */
    public boolean splitDocumentAt(int pageIndex)
    {
        synchronized (pages)
        {
            if (pageIndex < 0 || pageIndex >= pages.size() - 1)
            {
                return false;
            }

            // Insert a virtual split by rebuilding documents with a forced break
            rebuildDocumentsWithManualSplit(pageIndex);
            return true;
        }
    }

    private void rebuildDocumentsWithManualSplit(int splitAfterIndex)
    {
        List<Document> rebuilt = new ArrayList<>();
        Document current = new Document(rebuilt.size() + 1);
        rebuilt.add(current);

        for (int i = 0; i < pages.size(); i++)
        {
            Files page = pages.get(i);
            current.addPage(page);

            // Split after this index OR after a barcode
            if (i == splitAfterIndex || page.isBarcode())
            {
                if (i < pages.size() - 1)
                {
                    current = new Document(rebuilt.size() + 1);
                    rebuilt.add(current);
                }
            }
        }

        synchronized (documents)
        {
            documents.clear();
            documents.addAll(rebuilt);
        }

        documentCounter.set(rebuilt.size());
        startNewDocumentOnNextPage = !pages.isEmpty() && pages.get(pages.size() - 1).isBarcode();
    }


}
