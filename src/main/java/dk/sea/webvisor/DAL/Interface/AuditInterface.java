package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;

// Java Imports
import java.sql.SQLException;
import java.util.List;

public interface AuditInterface
{
    void insertAuditEntry(AuditEntry entry) throws SQLException;

    List<AuditEntry> getAllAuditEntries() throws SQLException;
}
