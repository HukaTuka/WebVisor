package dk.sea.webvisor.BE;

// Java Imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Document {
    private int id;
    private final int documentNumber;
    private final List<Files> pages = new ArrayList<>();

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

    @Override
    public String toString() {
        return "Document " + documentNumber + " (" + pages.size() + " page(s))";
    }

    public int getId() {return id;}
}