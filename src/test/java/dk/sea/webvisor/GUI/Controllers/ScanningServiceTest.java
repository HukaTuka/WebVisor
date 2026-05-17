package dk.sea.webvisor.GUI.Controllers;

import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.BLL.ScanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//We test the scanningcontrollers functions
class ScanningServiceTest {

    private ScanningService scanningService;

    // Helper: a real Files object that is NOT a barcode
    private Files makePage(int pageNumber) {
        return new Files(pageNumber, new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), false);
    }

    // Helper: a real Files object that IS a barcode (triggers a document split)
    private Files makeBarcode(int pageNumber) {
        return new Files(pageNumber, new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), true);
    }

    @BeforeEach
    void setUp() {
        scanningService = new ScanningService();
    }


    //barcode split test
    @Test
    void barcodeExists_Test() {
        // Arrange: page, barcode, page — should become two documents
        Files page1   = makePage(1);
        Files barcode = makeBarcode(2);
        Files page3   = makePage(3);
        scanningService.loadSessionPages(List.of(page1, barcode, page3));

        // Act
        List<Document> documents = scanningService.getDocuments();

        // Assert
        assertEquals(2, documents.size(),
                "A barcode should split the pages into two documents");

        List<Files> firstDoc = documents.get(0).getPages();
        assertTrue(firstDoc.contains(page1),   "First document should contain page 1");
        assertTrue(firstDoc.contains(barcode), "First document should contain the barcode page");

        List<Files> secondDoc = documents.get(1).getPages();
        assertTrue(secondDoc.contains(page3),  "Second document should contain page 3");
        System.out.println(documents + " " + firstDoc + " " + secondDoc);
    }

    @Test
    void noBarcode_Test() {
        // Arrange
        scanningService.loadSessionPages(List.of(makePage(1), makePage(2), makePage(3)));

        // Act
        List<Document> documents = scanningService.getDocuments();

        // Assert
        assertEquals(1, documents.size(),
                "Without a barcode all pages should belong to one document");
        assertEquals(3, documents.get(0).getPages().size(),
                "The single document should contain all three pages");
        System.out.println(documents);
    }

    @Test
    void splitDocumentButton_Test() {
        // Arrange: four plain pages in one document
        Files p1 = makePage(1);
        Files p2 = makePage(2);
        Files p3 = makePage(3);
        Files p4 = makePage(4);
        scanningService.loadSessionPages(List.of(p1, p2, p3, p4));
        assertEquals(1, scanningService.getDocuments().size(), "Should start as one document");

        // Act — split after index 1 (after page 2)
        boolean result = scanningService.splitDocumentAt(1);

        // Assert
        assertTrue(result, "splitDocumentAt should return true for a valid index");
        List<Document> documents = scanningService.getDocuments();
        assertEquals(2, documents.size(), "Should now be two documents");
        assertTrue(documents.get(0).getPages().contains(p1));
        assertTrue(documents.get(0).getPages().contains(p2));
        assertTrue(documents.get(1).getPages().contains(p3));
        assertTrue(documents.get(1).getPages().contains(p4));
        System.out.println(documents);
    }
}
