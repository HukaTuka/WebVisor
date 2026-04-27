package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.AuditInterface;

// Java Imports
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO implements AuditInterface
{
    public AuditDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public void insertAuditEntry(AuditEntry entry) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.AuditLog (Timestamp, Username, Action, Details)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setTimestamp(1, Timestamp.valueOf(entry.getTimestamp()));
            statement.setString(2, entry.getUsername());
            statement.setString(3, entry.getAction());
            statement.setString(4, entry.getDetails());
            statement.executeUpdate();
        }
    }

    @Override
    public List<AuditEntry> getAllAuditEntries() throws SQLException
    {
        String sql = """
                SELECT ID, Timestamp, Username, Action, Details
                FROM dbo.AuditLog
                ORDER BY Timestamp DESC
                """;

        List<AuditEntry> entries = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                entries.add(createAuditEntryFromResultSet(rs));
            }
        }

        return entries;
    }

    private AuditEntry createAuditEntryFromResultSet(ResultSet rs) throws SQLException
    {
        String username = rs.getString("Username");
        String action   = rs.getString("Action");
        String details  = rs.getString("Details");

        // Use the DB timestamp so the entry reflects what was stored,
        // not when the object was reconstructed in Java
        AuditEntry entry = new AuditEntry(username, action, details);
        entry.overrideTimestamp(rs.getTimestamp("Timestamp").toLocalDateTime());

        return entry;
    }
}