package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.BoxesInterface;

// Java Imports
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BoxesDAO implements BoxesInterface
{
    public BoxesDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public Optional<Boxes> getBoxById(String boxId) throws SQLException
    {
        String sql = """
                SELECT TOP 1 b.BoxID, b.ArchiveID, c.Name AS ClientName, a.Name AS ArchiveName
                FROM dbo.Boxes b
                LEFT JOIN dbo.Clients c ON c.ID = b.ClientID
                LEFT JOIN dbo.Archives a ON a.ID = b.ArchiveID
                WHERE BoxID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);

            try (ResultSet rs = statement.executeQuery())
            {
                if (!rs.next())
                {
                    return Optional.empty();
                }
                return Optional.of(new Boxes(
                        rs.getString("BoxID"),
                        rs.getInt("ArchiveID"),
                        rs.getString("ClientName"),
                        rs.getString("ArchiveName")
                ));
            }
        }
    }

    @Override
    public List<Boxes> getAllBoxes() throws SQLException
    {
        String sql = """
                SELECT b.BoxID, b.ArchiveID, c.Name AS ClientName, a.Name AS ArchiveName,
                       (SELECT COUNT(*) FROM dbo.Documents d WHERE d.BoxID = b.BoxID) AS DocumentCount,
                       (SELECT COUNT(*) FROM dbo.Files f WHERE f.BoxID = b.BoxID) AS FileCount
                FROM dbo.Boxes b
                LEFT JOIN dbo.Clients c ON c.ID = b.ClientID
                LEFT JOIN dbo.Archives a ON a.ID = b.ArchiveID
                ORDER BY b.BoxID
                """;

        List<Boxes> boxes = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                Boxes box = new Boxes(
                        rs.getString("BoxID"),
                        rs.getInt("ArchiveID"),
                        rs.getString("ClientName"),
                        rs.getString("ArchiveName")
                );
                box.setSummaryCounts(
                        rs.getInt("DocumentCount"),
                        rs.getInt("FileCount")
                );
                boxes.add(box);
            }
        }

        return boxes;
    }

    @Override
    public Boxes createBox(String boxId, int clientId, int archiveId) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Boxes (BoxID, ClientID, ArchiveID)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.setInt(2, clientId);
            statement.setInt(3, archiveId);
            statement.executeUpdate();
        }

        return getBoxById(boxId).orElse(new Boxes(boxId, ""));
    }

    @Override
    public void deleteBox(String boxId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Boxes WHERE BoxID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.executeUpdate();
        }
    }
}
