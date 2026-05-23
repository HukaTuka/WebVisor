package dk.sea.webvisor.GUI.Managers;

import dk.sea.webvisor.BE.*;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.BLL.ScanningService;
import dk.sea.webvisor.BLL.Util.AuditService;

import java.sql.SQLException;
import java.util.ArrayList;
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

    public void split(Boxes box,
                      List<Files> pages,
                      int index,
                      Runnable after)
    {
        if (index < 0 || index >= pages.size() - 1)
        {
            uiManager.error("Invalid split position.");
            return;
        }

        try
        {
            rebuildAndPersistSplit(box, index);
            reloadIntoSession(box, pages);

            audit.log("Split document manually", "Document split at page index "
                    + index + " in box " + box.getBoxId());

            after.run();
        }
        catch (SQLException e)
        {
            uiManager.error("Split failed: ");
        }
    }

    public void movePageToDocument(Boxes box,
                                   List<Files> pages,
                                   Files page,
                                   Document targetDoc,
                                   Runnable after)
    {
        int pageIndex = findPageIndex(pages, page);
        int docIndex  = findDocumentIndex(box.getDocuments(), targetDoc);

        if (pageIndex < 0) { uiManager.error("Could not find page.");             return; }
        if (docIndex  < 0) { uiManager.error("Could not find target document."); return; }

        try
        {
            scanningService.movePageToDocument(pageIndex, docIndex);

            pages.clear();
            pages.addAll(scanningService.getAllPages());
            box.replaceContent(pages, scanningService.getDocuments());

            if (page.getId() > 0)
            {
                persistPageMove(box.getBoxId(), page.getId(), targetDoc.getDocumentNumber());
            }

            audit.log("Page was moved", "Moved page " + page.getReferenceId()
                    + " to document " + targetDoc.getDocumentNumber()
                    + " in box " + box.getBoxId());

            after.run();
        }
        catch (SQLException e)
        {
            uiManager.error("Could not move page: " + e.getMessage());
        }
    }

    private void rebuildAndPersistSplit(Boxes box, int splitIndex) throws SQLException
    {
        List<Files>    pages    = new ArrayList<>(box.getPages());
        List<Document> existing = box.getDocuments();
        List<Document> rebuilt  = new ArrayList<>();

        int docNum = 1;
        Document current = new Document(0, docNum++);
        rebuilt.add(current);

        for (int i = 0; i < pages.size(); i++)
        {
            Files page = pages.get(i);
            current.addPage(page);

            boolean shouldSplit = (i == splitIndex)
                    || page.isBarcode()
                    || isDocumentBoundary(existing, pages, i);

            if (shouldSplit && i < pages.size() - 1)
            {
                current = new Document(0, docNum++);
                rebuilt.add(current);
            }
        }

        box.replaceContent(pages, rebuilt);
        archiveService.saveBoxSnapshot(box);
    }

    private boolean isDocumentBoundary(List<Document> documents,
                                       List<Files> pages,
                                       int pageIndex)
    {
        if (pageIndex >= pages.size() - 1) return false;

        Files current = pages.get(pageIndex);
        Files next    = pages.get(pageIndex + 1);

        for (Document doc : documents)
        {
            List<Files> docPages = doc.getPages();
            if (docPages.isEmpty()) continue;

            Files last = docPages.get(docPages.size() - 1);
            if (isSamePage(last, current) && !isSamePage(last, next))
            {
                return true;
            }
        }

        return false;
    }

    private void persistPageMove(String boxId, int fileId, int toDocumentNumber)
            throws SQLException
    {
        java.util.Optional<Integer> toDocId =
                archiveService.getDocumentId(boxId, toDocumentNumber);

        if (toDocId.isEmpty())
        {
            throw new SQLException("Target document not found: " + toDocumentNumber);
        }

        archiveService.updateFileDocument(fileId, toDocId.get());
    }

    private void reloadIntoSession(Boxes box, List<Files> pages) throws SQLException
    {
        Boxes reloaded = archiveService.loadBoxContent(box.getBoxId());
        box.replaceContent(reloaded.getPages(), reloaded.getDocuments());
        pages.clear();
        pages.addAll(box.getPages());
        scanningService.loadSessionPages(pages);
    }

    private int findPageIndex(List<Files> pages, Files page)
    {
        if (page.getId() > 0)
        {
            for (int i = 0; i < pages.size(); i++)
            {
                if (pages.get(i).getId() == page.getId()) return i;
            }
        }
        return pages.indexOf(page);
    }

    private int findDocumentIndex(List<Document> documents, Document target)
    {
        for (int i = 0; i < documents.size(); i++)
        {
            Document doc = documents.get(i);
            if (doc == target) return i;
            if (doc.getId() > 0 && target.getId() > 0
                    && doc.getId() == target.getId()) return i;
            if (doc.getDocumentNumber() == target.getDocumentNumber()) return i;
        }
        return -1;
    }

    private boolean isSamePage(Files a, Files b)
    {
        if (a.getId() > 0 && b.getId() > 0) return a.getId() == b.getId();
        return a == b;
    }
}