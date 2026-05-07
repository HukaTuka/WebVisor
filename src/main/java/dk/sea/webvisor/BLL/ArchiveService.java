package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.DAL.DAO.BoxesDAO;
import dk.sea.webvisor.DAL.DAO.DocumentsDAO;
import dk.sea.webvisor.DAL.DAO.FilesDAO;
import dk.sea.webvisor.DAL.Interface.BoxesInterface;
import dk.sea.webvisor.DAL.Interface.DocumentsInterface;
import dk.sea.webvisor.DAL.Interface.FilesInterface;

// Java Imports
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArchiveService
{
    private final BoxesInterface boxesDAO;
    private final DocumentsInterface documentsDAO;
    private final FilesInterface filesDAO;

    public ArchiveService() throws IOException
    {
        this.boxesDAO = new BoxesDAO();
        this.documentsDAO = new DocumentsDAO();
        this.filesDAO = new FilesDAO();
    }

    public List<Boxes> getAllBoxesWithContent() throws SQLException
    {
        List<Boxes> boxes = new ArrayList<>(boxesDAO.getAllBoxes());

        for (Boxes box : boxes)
        {
            hydrateBox(box);
        }

        return boxes;
    }

    public List<Boxes> getAllBoxes() throws SQLException
    {
        return new ArrayList<>(boxesDAO.getAllBoxes());
    }

    public Boxes loadBoxContent(String boxId) throws SQLException
    {
        Optional<Boxes> maybeBox = boxesDAO.getBoxById(boxId);
        if (maybeBox.isEmpty())
        {
            throw new SQLException("Box not found: " + boxId);
        }

        Boxes box = maybeBox.get();
        hydrateBox(box);
        return box;
    }

    public Boxes createBox(String boxId) throws SQLException
    {
        Optional<Boxes> existing = boxesDAO.getBoxById(boxId);
        if (existing.isPresent())
        {
            return existing.get();
        }

        return boxesDAO.createBox(boxId);
    }

    public void saveBoxSnapshot(Boxes box) throws SQLException
    {
        if (box == null)
        {
            throw new IllegalArgumentException("Box is required.");
        }

        createBox(box.getBoxId());

        filesDAO.deleteFilesByBox(box.getBoxId());
        documentsDAO.deleteDocumentsByBox(box.getBoxId());

        int persistedPageNumber = 1;
        for (Document document : box.getDocuments())
        {
            int documentId = documentsDAO.createDocument(box.getBoxId(), document.getDocumentNumber());
            for (Files page : document.getPages())
            {
                Files pageToPersist = new Files(
                        0,
                        persistedPageNumber,
                        page.getImage(),
                        page.isBarcode(),
                        page.getRotationDegrees()
                );
                filesDAO.createFile(box.getBoxId(), documentId, pageToPersist);
                persistedPageNumber++;
            }
        }
    }

    public BufferedImage loadFileImage(int fileId) throws SQLException
    {
        return filesDAO.getFileImageById(fileId);
    }

    public void updatePageOrder(String boxId, List<Files> orderedPages) throws SQLException
    {
        List<Integer> fileIds = new ArrayList<>();
        for (Files page : orderedPages)
        {
            if (page.getId() > 0)
            {
                fileIds.add(page.getId());
            }
        }

        filesDAO.updatePageOrder(boxId, fileIds);
    }

    private void hydrateBox(Boxes box) throws SQLException
    {
        List<Document> documents = documentsDAO.getDocumentsByBox(box.getBoxId());
        for (Document document : documents)
        {
            Optional<Integer> documentId = documentsDAO.getDocumentId(box.getBoxId(), document.getDocumentNumber());
            if (documentId.isEmpty())
            {
                continue;
            }

            for (Files page : filesDAO.getFilesByDocument(documentId.get()))
            {
                document.addPage(page);
            }
        }

        List<Files> pages = filesDAO.getFilesByBox(box.getBoxId());
        box.replaceContent(pages, documents);
    }
    /**
     * Inserts a manual document split after the page at splitIndex
     * within the given box, then saves the result to the database.
     */
    public void splitDocumentAt(Boxes box, int splitIndex) throws SQLException
    {
        List<Files> pages = new ArrayList<>(box.getPages());

        if (splitIndex < 0 || splitIndex >= pages.size() - 1)
        {
            throw new IllegalArgumentException("Cannot split at this position.");
        }

        // Rebuild documents with the manual split point
        List<Document> rebuilt = new ArrayList<>();
        Document current = new Document(rebuilt.size() + 1);
        rebuilt.add(current);

        for (int i = 0; i < pages.size(); i++)
        {
            Files page = pages.get(i);
            current.addPage(page);

            boolean isSplitPoint = (i == splitIndex);
            boolean isBarcode    = page.isBarcode();

            if ((isSplitPoint || isBarcode) && i < pages.size() - 1)
            {
                current = new Document(rebuilt.size() + 1);
                rebuilt.add(current);
            }
        }

        box.replaceContent(pages, rebuilt);
        saveBoxSnapshot(box);
    }
}
