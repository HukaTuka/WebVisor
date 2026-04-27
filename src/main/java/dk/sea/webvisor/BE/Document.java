package dk.sea.webvisor.BE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Document {
    private final int documentNumber;
    private final List<ScannedPage> pages = new ArrayList<>();

    public Document(int documentNumber) {
        this.documentNumber = documentNumber;
    }

    public void addPage(ScannedPage page) {
        pages.add(page);
    }

    public List<ScannedPage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public int getDocumentNumber() {
        return documentNumber;
    }

    @Override
    public String toString() {
        return "Document " + documentNumber + " (" + pages.size() + " page(s))";
    }
}