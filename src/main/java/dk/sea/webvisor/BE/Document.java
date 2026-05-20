package dk.sea.webvisor.BE;

// Java Imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Document {
    private int id;
    private final int documentNumber;
    private final List<Files> pages = new ArrayList<>();
    private DocumentStatus status = DocumentStatus.IN_PROGRESS;

    public Document(int id, int documentNumber) {
        this.id = id;
        this.documentNumber = documentNumber;
    }

    public void addPage(Files page) {
        pages.add(page);
    }

    public List<Files> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public int getDocumentNumber() {
        return documentNumber;
    }

    public int getId()
    {
        return id;
    }

    public DocumentStatus getStatus()
    {
        return status;
    }

    public void setStatus(DocumentStatus status)
    {
        this.status = status == null ? DocumentStatus.IN_PROGRESS : status;
    }

    @Override
    public String toString()
    {
        String label = "Document " + documentNumber + " (" + pages.size() + " page(s))";
        if (status != DocumentStatus.IN_PROGRESS)
        {
            label += " [" + status.getDisplayName() + "]";
        }
        return label;
    }
}