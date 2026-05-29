package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.Archive;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.ArchivesInterface;

// Java Imports
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArchivesDAO implements ArchivesInterface
{
    public ArchivesDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public List<Archive> getAllArchives() throws SQLException
    {
        String sql = """
        SELECT a.ID, a.ClientID, a.Name, c.Name AS ClientName
        FROM dbo.Archives a
        INNER JOIN dbo.Clients c ON c.ID = a.ClientID
        WHERE c.IsDeleted = 0
        ORDER BY c.Name, a.Name
        """;

        List<Archive> archives = new ArrayList<>();
        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                archives.add(mapRow(rs));
            }
        }
        return archives;
    }

    @Override
    public List<Archive> getArchivesByClient(int clientId) throws SQLException
    {
        String sql = """
        SELECT a.ID, a.ClientID, a.Name, c.Name AS ClientName
        FROM dbo.Archives a
        INNER JOIN dbo.Clients c ON c.ID = a.ClientID
        WHERE c.IsDeleted = 0
        ORDER BY c.Name, a.Name
        """;

        List<Archive> archives = new ArrayList<>();
        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    archives.add(mapRow(rs));
                }
            }
        }
        return archives;
    }

    @Override
    public Optional<Archive> getArchiveByClientAndName(int clientId, String name) throws SQLException
    {
        String sql = """
        SELECT a.ID, a.ClientID, a.Name, c.Name AS ClientName
        FROM dbo.Archives a
        INNER JOIN dbo.Clients c ON c.ID = a.ClientID
        WHERE c.IsDeleted = 0
        ORDER BY c.Name, a.Name
        """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, clientId);
            statement.setString(2, name);

            try (ResultSet rs = statement.executeQuery())
            {
                if (!rs.next())
                {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    @Override
    public Archive createArchive(String name, int clientId) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Archives (Name, ClientID)
                VALUES (?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, name);
            statement.setInt(2, clientId);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    int archiveId = keys.getInt(1);
                    String clientName = getClientNameById(connection, clientId);
                    return new Archive(archiveId, clientId, name, clientName);
                }
            }
        }

        Optional<Archive> existing = getArchiveByClientAndName(clientId, name);
        if (existing.isPresent())
        {
            return existing.get();
        }

        throw new SQLException("Could not create archive: " + name);
    }

    @Override
    public void updateArchive(Archive archive) throws SQLException
    {
        String sql = """
                UPDATE dbo.Archives
                SET Name = ?, ClientID = ?
                WHERE ID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, archive.getName());
            statement.setInt(2, archive.getClientId());
            statement.setInt(3, archive.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteArchive(int archiveId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Archives WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, archiveId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<String> getBoxIdsAssignedToArchive(int archiveId) throws SQLException
    {
        String sql = """
                SELECT BoxID
                FROM dbo.Boxes
                WHERE ArchiveID = ?
                ORDER BY BoxID
                """;

        List<String> boxIds = new ArrayList<>();
        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, archiveId);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    boxIds.add(rs.getString("BoxID"));
                }
            }
        }
        return boxIds;
    }

    private Archive mapRow(ResultSet rs) throws SQLException
    {
        return new Archive(
                rs.getInt("ID"),
                rs.getInt("ClientID"),
                rs.getString("Name"),
                rs.getString("ClientName")
        );
    }

    private String getClientNameById(Connection connection, int clientId) throws SQLException
    {
        String sql = "SELECT Name FROM dbo.Clients WHERE ID = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getString("Name");
                }
            }
        }
        return "";
    }
}
