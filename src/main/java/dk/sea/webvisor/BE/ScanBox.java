package dk.sea.webvisor.BE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScanBox
{
    private final String boxId;
    private final List<ScannedPage> pages = new ArrayList<>();
    private final List<Document> documents = new ArrayList<>();

    public ScanBox(String boxId)
    {
        this.boxId = boxId;
    }

    public String getBoxId()
    {
        return boxId;
    }

    public List<ScannedPage> getPages()
    {
        return Collections.unmodifiableList(pages);
    }

    public List<Document> getDocuments()
    {
        return Collections.unmodifiableList(documents);
    }

    public void clearContent()
    {
        pages.clear();
        documents.clear();
    }

    public void replaceContent(List<ScannedPage> newPages, List<Document> newDocuments)
    {
        pages.clear();
        pages.addAll(newPages);
        documents.clear();
        documents.addAll(newDocuments);
    }

    @Override
    public String toString()
    {
        return boxId + " (" + documents.size() + " docs, " + pages.size() + " files)";
    }
}
