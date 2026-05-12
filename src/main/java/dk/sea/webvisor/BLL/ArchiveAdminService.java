package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Archive;
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.DAL.DAO.ArchivesDAO;
import dk.sea.webvisor.DAL.Interface.ArchivesInterface;

// Java Imports
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ArchiveAdminService
{
    private final ArchivesInterface archivesDAO;

    public ArchiveAdminService() throws IOException
    {
        this.archivesDAO = new ArchivesDAO();
    }

    public List<Archive> getAllArchives() throws SQLException
    {
        return archivesDAO.getAllArchives();
    }

    public Archive createArchive(String name, Client client) throws SQLException
    {
        if (client == null)
        {
            throw new IllegalArgumentException("Client must be selected.");
        }

        String cleaned = validateName(name);
        Optional<Archive> existing = archivesDAO.getArchiveByClientAndName(client.getId(), cleaned);
        if (existing.isPresent())
        {
            throw new IllegalArgumentException("Archive already exists for this client.");
        }
        return archivesDAO.createArchive(cleaned, client.getId());
    }

    public void updateArchive(int archiveId, String name, Client client) throws SQLException
    {
        if (archiveId <= 0)
        {
            throw new IllegalArgumentException("Select an archive from the table first.");
        }
        if (client == null)
        {
            throw new IllegalArgumentException("Client must be selected.");
        }

        String cleaned = validateName(name);
        Optional<Archive> existing = archivesDAO.getArchiveByClientAndName(client.getId(), cleaned);
        if (existing.isPresent() && existing.get().getId() != archiveId)
        {
            throw new IllegalArgumentException("Archive name is already in use for this client.");
        }

        archivesDAO.updateArchive(new Archive(archiveId, client.getId(), cleaned, client.getName()));
    }

    public void deleteArchive(int archiveId) throws SQLException
    {
        if (archiveId <= 0)
        {
            throw new IllegalArgumentException("Select an archive from the table first.");
        }

        List<String> boxes = archivesDAO.getBoxIdsAssignedToArchive(archiveId);
        if (!boxes.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Archive cannot be deleted while boxes are assigned: " + String.join(", ", boxes));
        }

        archivesDAO.deleteArchive(archiveId);
    }

    public List<String> getBoxIdsAssignedToArchive(int archiveId) throws SQLException
    {
        return archivesDAO.getBoxIdsAssignedToArchive(archiveId);
    }

    private String validateName(String name)
    {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isBlank())
        {
            throw new IllegalArgumentException("Archive name must not be empty.");
        }
        if (cleaned.length() > 150)
        {
            throw new IllegalArgumentException("Archive name must not exceed 150 characters.");
        }
        return cleaned;
    }
}
