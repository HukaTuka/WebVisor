package dk.sea.webvisor.DAL.Interface;

import dk.sea.webvisor.BE.AuditEntry;

import java.sql.SQLException;
import java.util.List;

public interface AuditInterface
{
    void insertAuditEntry(AuditEntry entry) throws SQLException;

    List<AuditEntry> getAllAuditEntries() throws SQLException;
}
