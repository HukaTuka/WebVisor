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
                      ScanningSessionManager session,
                      Runnable after)
    {
        if (index < 0 || index >= pages.size() - 1)
        {
            uiManager.error("Invalid split position.");
            return;
        }

        try
        {
            splitDocumentAt(box, index);

            Boxes reloaded = archiveService.loadBoxContent(box.getBoxId());
            box.replaceContent(reloaded.getPages(), reloaded.getDocuments());

            pages.clear();
            pages.addAll(box.getPages());
            scanningService.loadSessionPages(pages);

            audit.log("SPLIT_DOCUMENT", "Document split at page index " + index
                    + " in box " + box.getBoxId());

            after.run();
        }
        catch (SQLException e)
        {
            uiManager.error("Split failed: " + e.getMessage());
        }
    }

    private void splitDocumentAt(Boxes box, int splitIndex) throws SQLException
    {
        List<Files> pages = new ArrayList<>(box.getPages());

        if (splitIndex < 0 || splitIndex >= pages.size() - 1)
        {
            throw new IllegalArgumentException("Cannot split at this position.");
        }

        List<Document> existing = box.getDocuments();
        List<Document> rebuilt  = new ArrayList<>();

        int docNum = 1;
        Document current = new Document(0, docNum++);
        rebuilt.add(current);

        for (int i = 0; i < pages.size(); i++)
        {
            Files page = pages.get(i);
            current.addPage(page);

            boolean manualSplit  = (i == splitIndex);
            boolean barcodeSplit = page.isBarcode();
            boolean docBoundary  = isDocumentBoundary(existing, pages, i);

            boolean shouldSplit  = manualSplit || barcodeSplit || docBoundary;

            if (shouldSplit && i < pages.size() - 1)
            {
                current = new Document(0, docNum++);
                rebuilt.add(current);
            }
        }

        box.replaceContent(pages, rebuilt);
        archiveService.saveBoxSnapshot(box);
    }

    private boolean isDocumentBoundary(List<Document> documents, List<Files> pages, int pageIndex)
    {
        if (pageIndex >= pages.size() - 1) return false;

        Files currentPage = pages.get(pageIndex);
        Files nextPage    = pages.get(pageIndex + 1);

        for (Document doc : documents)
        {
            List<Files> docPages = doc.getPages();
            if (docPages.isEmpty()) continue;

            Files lastPageOfDoc = docPages.get(docPages.size() - 1);

            if (isSamePage(lastPageOfDoc, currentPage) && !isSamePage(lastPageOfDoc, nextPage))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isSamePage(Files a, Files b)
    {
        if (a.getId() > 0 && b.getId() > 0) return a.getId() == b.getId();
        return a == b;
    }
}