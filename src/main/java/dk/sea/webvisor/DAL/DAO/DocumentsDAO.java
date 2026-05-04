package dk.sea.webvisor.DAL.DAO;

import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.DocumentsInterface;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DocumentsDAO implements DocumentsInterface
{
    public DocumentsDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public int createDocument(String boxId, int documentNumber) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Documents (BoxID, DocumentNumber)
                VALUES (?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, boxId);
            statement.setInt(2, documentNumber);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not read generated document ID.");
    }

    @Override
    public Optional<Integer> getDocumentId(String boxId, int documentNumber) throws SQLException
    {
        String sql = """
                SELECT TOP 1 ID
                FROM dbo.Documents
                WHERE BoxID = ? AND DocumentNumber = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.setInt(2, documentNumber);

            try (ResultSet rs = statement.executeQuery())
            {
                if (!rs.next())
                {
                    return Optional.empty();
                }
                return Optional.of(rs.getInt("ID"));
            }
        }
    }

    @Override
    public List<Document> getDocumentsByBox(String boxId) throws SQLException
    {
        String sql = """
                SELECT ID, DocumentNumber
                FROM dbo.Documents
                WHERE BoxID = ?
                ORDER BY DocumentNumber
                """;

        List<Document> documents = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);

            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    documents.add(new Document(rs.getInt("DocumentNumber")));
                }
            }
        }

        return documents;
    }

    @Override
    public void deleteDocument(int documentId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Documents WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, documentId);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteDocumentsByBox(String boxId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Documents WHERE BoxID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.executeUpdate();
        }
    }
}
