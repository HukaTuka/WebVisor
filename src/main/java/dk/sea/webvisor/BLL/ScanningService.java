package dk.sea.webvisor.BLL;

import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.Util.BarcodeDetector;
import dk.sea.webvisor.DAL.API.TiffApiClient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanningService
{
    private final TiffApiClient apiClient;
    private final List<Files> pages;
    private final AtomicInteger pageCounter;

    private final List<Document> documents = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger documentCounter = new AtomicInteger(0);

    private boolean startNewDocumentOnNextPage = false;

    public ScanningService()
    {
        this.apiClient   = new TiffApiClient();
        this.pages       = Collections.synchronizedList(new ArrayList<>());
        this.pageCounter = new AtomicInteger(0);
    }

    public List<Files> fetchAndAppendNext() throws IOException {
        List<BufferedImage> images = apiClient.fetchRandomPage();
        List<Files> newPages = new ArrayList<>();

        for (BufferedImage image : images) {
            boolean barcode = BarcodeDetector.isBarcode(image);
            Files page = new Files(pageCounter.incrementAndGet(), image, barcode);
            pages.add(page);
            newPages.add(page);

            if (documents.isEmpty()) {
                documents.add(new Document(0, documentCounter.incrementAndGet()));
            }

            if (startNewDocumentOnNextPage) {
                documents.add(new Document(0, documentCounter.incrementAndGet()));
                startNewDocumentOnNextPage = false;
            }

            documents.get(documents.size() - 1).addPage(page);

            if (barcode) {
                startNewDocumentOnNextPage = true;
            }
        }

        return newPages;
    }

    public List<Files> getAllPages()
    {
        synchronized (pages)
        {
            return List.copyOf(pages);
        }
    }

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

    public void loadSessionPages(List<Files> existingPages)
    {
        synchronized (pages)
        {
            pages.clear();
            pages.addAll(existingPages);
            rebuildDocumentsFromPages();
            pageCounter.set(pages.size());
        }
    }

    public boolean deletePageAt(int pageIndex)
    {
        synchronized (pages)
        {
            if (pageIndex < 0 || pageIndex >= pages.size())
                return false;

            pages.remove(pageIndex);
            rebuildDocumentsFromPages();
            return true;
        }
    }

    public boolean movePage(int fromIndex, int toIndex)
    {
        synchronized (pages)
        {
            if (fromIndex < 0 || fromIndex >= pages.size() ||
                    toIndex   < 0 || toIndex   >= pages.size())
                return false;

            if (fromIndex == toIndex)
                return true;

            Files movedPage = pages.remove(fromIndex);
            pages.add(toIndex, movedPage);

            reorderPagesWithinExistingDocuments();
            return true;
        }
    }

    public boolean movePageToDocument(int pageIndex, int targetDocumentIndex)
    {
        synchronized (pages)
        {
            if (pageIndex < 0 || pageIndex >= pages.size())
                return false;

            synchronized (documents)
            {
                if (targetDocumentIndex < 0 || targetDocumentIndex >= documents.size())
                    return false;

                Files pageToMove = pages.get(pageIndex);

                int sourceDocumentIndex = -1;

                outer:
                for (int i = 0; i < documents.size(); i++)
                {
                    for (Files p : documents.get(i).getPages())
                    {
                        if (p == pageToMove)
                        {
                            sourceDocumentIndex = i;
                            break outer;
                        }
                    }
                }

                if (sourceDocumentIndex == targetDocumentIndex)
                    return false;

                List<List<Files>> buckets = new ArrayList<>();

                for (Document d : documents)
                {
                    List<Files> bucket = new ArrayList<>();
                    for (Files p : d.getPages())
                    {
                        if (p != pageToMove)
                            bucket.add(p);
                    }
                    buckets.add(bucket);
                }

                buckets.get(targetDocumentIndex).add(pageToMove);

                List<Document> finalDocs = new ArrayList<>();

                for (List<Files> bucket : buckets)
                {
                    if (bucket.isEmpty()) continue;

                    Document d = new Document(0, finalDocs.size() + 1);

                    for (Files p : bucket)
                        d.addPage(p);

                    finalDocs.add(d);
                }

                documents.clear();
                documents.addAll(finalDocs);
                documentCounter.set(finalDocs.size());

                List<Files> reordered = new ArrayList<>();
                for (Document d : finalDocs)
                    reordered.addAll(d.getPages());

                pages.clear();
                pages.addAll(reordered);

                startNewDocumentOnNextPage =
                        !pages.isEmpty() && pages.get(pages.size() - 1).isBarcode();

                return true;
            }
        }
    }

    private void reorderPagesWithinExistingDocuments()
    {
        Map<Files, Integer> documentByPage = new IdentityHashMap<>();

        synchronized (documents)
        {
            for (int docIndex = 0; docIndex < documents.size(); docIndex++)
            {
                for (Files page : documents.get(docIndex).getPages())
                {
                    documentByPage.put(page, docIndex);
                }
            }
        }

        if (documentByPage.isEmpty())
        {
            rebuildDocumentsFromPages();
            return;
        }

        List<Document> rebuilt = new ArrayList<>();

        synchronized (documents)
        {
            for (int i = 0; i < documents.size(); i++)
            {
                rebuilt.add(new Document(0, i + 1));
            }
        }

        for (Files page : pages)
        {
            int targetDocIndex = documentByPage.getOrDefault(page, 0);

            rebuilt.get(targetDocIndex).addPage(page);
        }

        synchronized (documents)
        {
            documents.clear();
            documents.addAll(rebuilt);
        }

        documentCounter.set(rebuilt.size());
        startNewDocumentOnNextPage =
                !pages.isEmpty() && pages.get(pages.size() - 1).isBarcode();
    }

    private void rebuildDocumentsFromPages()
    {
        List<Document> rebuilt = new ArrayList<>();
        Document current = null;

        for (Files page : pages)
        {
            if (current == null)
            {
                current = new Document(0, rebuilt.size() + 1);
                rebuilt.add(current);
            }

            current.addPage(page);

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
        startNewDocumentOnNextPage =
                !pages.isEmpty() && pages.get(pages.size() - 1).isBarcode();
    }

    public int getPageCount()
    {
        return pageCounter.get();
    }

    public boolean splitDocumentAt(int pageIndex)
    {
        synchronized (pages)
        {
            if (pageIndex < 0 || pageIndex >= pages.size() - 1)
                return false;

            rebuildDocumentsWithManualSplit(pageIndex);
            return true;
        }
    }

    private void rebuildDocumentsWithManualSplit(int splitAfterIndex)
    {
        List<Document> rebuilt = new ArrayList<>();

        Document current = new Document(0, 1);
        rebuilt.add(current);

        for (int i = 0; i < pages.size(); i++)
        {
            Files page = pages.get(i);
            current.addPage(page);

            if (i == splitAfterIndex || page.isBarcode())
            {
                if (i < pages.size() - 1)
                {
                    current = new Document(0, rebuilt.size() + 1);
                    rebuilt.add(current);
                }
            }
        }

        documents.clear();
        documents.addAll(rebuilt);
        documentCounter.set(rebuilt.size());

        startNewDocumentOnNextPage =
                !pages.isEmpty() && pages.get(pages.size() - 1).isBarcode();
    }
}