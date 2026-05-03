package dk.sea.webvisor.BE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Boxes
{
    private final String boxId;
    private final List<Files> pages = new ArrayList<>();
    private final List<Document> documents = new ArrayList<>();
    private int documentCount = 0;
    private int fileCount = 0;

    public Boxes(String boxId)
    {
        this.boxId = boxId;
    }

    public String getBoxId()
    {
        return boxId;
    }

    public List<Files> getPages()
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

    public void replaceContent(List<Files> newPages, List<Document> newDocuments)
    {
        pages.clear();
        pages.addAll(newPages);
        documents.clear();
        documents.addAll(newDocuments);
        documentCount = documents.size();
        fileCount = pages.size();
    }

    public void setSummaryCounts(int documents, int files)
    {
        this.documentCount = Math.max(0, documents);
        this.fileCount = Math.max(0, files);
    }

    @Override
    public String toString()
    {
        return boxId + " (" + documentCount + " docs, " + fileCount + " files)";
    }
}
