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

        for (Document document : box.getDocuments())
        {
            int documentId = documentsDAO.createDocument(box.getBoxId(), document.getDocumentNumber());
            for (Files page : document.getPages())
            {
                filesDAO.createFile(box.getBoxId(), documentId, page);
            }
        }
    }

    public BufferedImage loadFileImage(int fileId) throws SQLException
    {
        return filesDAO.getFileImageById(fileId);
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
}
