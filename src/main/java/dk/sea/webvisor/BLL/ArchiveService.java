package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.DocumentStatus;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BE.Archive;
import dk.sea.webvisor.DAL.DAO.ArchivesDAO;
import dk.sea.webvisor.DAL.DAO.BoxesDAO;
import dk.sea.webvisor.DAL.DAO.ClientsDAO;
import dk.sea.webvisor.DAL.DAO.DocumentsDAO;
import dk.sea.webvisor.DAL.DAO.FilesDAO;
import dk.sea.webvisor.DAL.Interface.ArchivesInterface;
import dk.sea.webvisor.DAL.Interface.BoxesInterface;
import dk.sea.webvisor.DAL.Interface.ClientsInterface;
import dk.sea.webvisor.DAL.Interface.DocumentsInterface;
import dk.sea.webvisor.DAL.Interface.FilesInterface;

// Java Imports
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArchiveService {
    private final BoxesInterface boxesDAO;
    private final ClientsInterface clientsDAO;
    private final ArchivesInterface archivesDAO;
    private final DocumentsInterface documentsDAO;
    private final FilesInterface filesDAO;

    public ArchiveService() throws IOException {
        this.boxesDAO = new BoxesDAO();
        this.clientsDAO = new ClientsDAO();
        this.archivesDAO = new ArchivesDAO();
        this.documentsDAO = new DocumentsDAO();
        this.filesDAO = new FilesDAO();
    }

    public List<Boxes> getAllBoxesWithContent() throws SQLException {
        List<Boxes> boxes = new ArrayList<>(boxesDAO.getAllBoxes());

        for (Boxes box : boxes) {
            hydrateBox(box);
        }

        return boxes;
    }

    public List<Boxes> getAllBoxes() throws SQLException {
        return new ArrayList<>(boxesDAO.getAllBoxes());
    }

    public List<Client> getAllClients() throws SQLException {
        return new ArrayList<>(clientsDAO.getAllClients());
    }

    public List<Archive> getAllArchives() throws SQLException {
        return new ArrayList<>(archivesDAO.getAllArchives());
    }

    public List<Archive> getArchivesByClient(int clientId) throws SQLException {
        return new ArrayList<>(archivesDAO.getArchivesByClient(clientId));
    }

    public Boxes loadBoxContent(String boxId) throws SQLException {
        Optional<Boxes> maybeBox = boxesDAO.getBoxById(boxId);
        if (maybeBox.isEmpty()) {
            throw new SQLException("Box not found: " + boxId);
        }

        Boxes box = maybeBox.get();
        hydrateBox(box);
        return box;
    }

    public Boxes createBox(String boxId, String client, String archive) throws SQLException {
        Optional<Boxes> existing = boxesDAO.getBoxById(boxId);
        if (existing.isPresent()) {
            return existing.get();
        }

        String clientName = client == null ? "" : client.trim();
        if (clientName.isBlank()) {
            throw new IllegalArgumentException("Client is required.");
        }

        String archiveName = archive == null ? "" : archive.trim();
        if (archiveName.isBlank()) {
            throw new IllegalArgumentException("Archive is required.");
        }

        Optional<Client> existingClient = clientsDAO.getClientByName(clientName);
        Client resolved = existingClient.isPresent()
                ? existingClient.get()
                : clientsDAO.createClient(clientName);

        Optional<Archive> existingArchive = archivesDAO.getArchiveByClientAndName(resolved.getId(), archiveName);
        if (existingArchive.isEmpty()) {
            throw new IllegalArgumentException("Archive does not exist for selected client.");
        }

        Archive selectedArchive = existingArchive.get();
        Boxes persisted = boxesDAO.createBox(boxId, resolved.getId(), selectedArchive.getId());
        return new Boxes(persisted.getBoxId(), selectedArchive.getId(), resolved.getName(), selectedArchive.getName());
    }

    public void saveBoxSnapshot(Boxes box) throws SQLException {
        if (box == null) {
            throw new IllegalArgumentException("Box is required.");
        }

        Optional<Boxes> existing = boxesDAO.getBoxById(box.getBoxId());
        if (existing.isEmpty()) {
            if (box.getArchiveId() <= 0) {
                throw new IllegalArgumentException("Archive is required for box persistence.");
            }

            Optional<Client> existingClient = clientsDAO.getClientByName(box.getClient());
            if (existingClient.isEmpty()) {
                throw new IllegalArgumentException("Client not found: " + box.getClient());
            }
            boxesDAO.createBox(box.getBoxId(), existingClient.get().getId(), box.getArchiveId());
        }

        filesDAO.clearDocumentLinksByBox(box.getBoxId());
        documentsDAO.deleteDocumentsByBox(box.getBoxId());

        for (Document document : box.getDocuments()) {
            int documentId = documentsDAO.createDocument(box.getBoxId(), document.getDocumentNumber());

            if (document.getStatus() != DocumentStatus.IN_PROGRESS) {
                documentsDAO.updateDocumentStatus(documentId, document.getStatus());
            }

            for (Files page : document.getPages()) {
                if (page.getId() > 0) {
                    filesDAO.updateFileDocument(page.getId(), documentId);
                } else if (page.getImage() != null) {
                    filesDAO.createFile(box.getBoxId(), documentId, page);
                }
            }
        }
    }

    public BufferedImage loadFileImage(int fileId) throws SQLException {
        return filesDAO.getFileImageById(fileId);
    }

    public void updatePageOrder(String boxId, List<Files> orderedPages) throws SQLException {
        List<Integer> fileIds = new ArrayList<>();
        for (Files page : orderedPages) {
            if (page.getId() > 0) {
                fileIds.add(page.getId());
            }
        }

        filesDAO.updatePageOrder(boxId, fileIds);
    }

    public void deleteBox(String boxId) throws SQLException {
        boxesDAO.deleteBox(boxId);
    }

    public void deleteFileById(int fileId) throws SQLException {
        filesDAO.deleteFile(fileId);
    }

    public void deleteDocumentByNumber(String boxId, int documentNumber) throws SQLException {
        Optional<Integer> documentId = documentsDAO.getDocumentId(boxId, documentNumber);
        if (documentId.isPresent()) {
            documentsDAO.deleteDocument(documentId.get());
        }
    }

    /**
     * Updates the status of a document in the database.
     *
     * @param documentId the database ID of the document to update.
     * @param status     the new status to apply.
     * @throws SQLException if the database operation fails.
     */
    public void updateDocumentStatus(int documentId, DocumentStatus status) throws SQLException {
        documentsDAO.updateDocumentStatus(documentId, status);
    }

    private void hydrateBox(Boxes box) throws SQLException {
        List<Document> documents = documentsDAO.getDocumentsByBox(box.getBoxId());

        List<Files> allPages = new ArrayList<>();

        for (Document document : documents) {
            int documentId = document.getId();
            List<Files> pages = filesDAO.getFilesByDocument(documentId);

            for (Files page : pages) {
                document.addPage(page);
                allPages.add(page);
            }
        }

        box.replaceContent(allPages, documents);
    }

}