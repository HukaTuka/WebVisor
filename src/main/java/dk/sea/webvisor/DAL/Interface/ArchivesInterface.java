package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.Archive;

// Java Imports
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ArchivesInterface
{
    List<Archive> getAllArchives() throws SQLException;
    List<Archive> getArchivesByClient(int clientId) throws SQLException;
    Optional<Archive> getArchiveByClientAndName(int clientId, String name) throws SQLException;
    Archive createArchive(String name, int clientId) throws SQLException;
    void updateArchive(Archive archive) throws SQLException;
    void deleteArchive(int archiveId) throws SQLException;
    List<String> getBoxIdsAssignedToArchive(int archiveId) throws SQLException;
}
