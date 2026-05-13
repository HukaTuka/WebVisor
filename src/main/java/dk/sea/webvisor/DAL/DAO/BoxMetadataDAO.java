package dk.sea.webvisor.DAL.DAO;

import dk.sea.webvisor.BE.BoxMetadataEntry;
import dk.sea.webvisor.BE.MetadataField;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.BoxMetadataInterface;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoxMetadataDAO implements BoxMetadataInterface
{
    public BoxMetadataDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public void saveMetadata(String boxId, int fieldId, String value) throws SQLException
    {
        String sql = """
                IF EXISTS (SELECT 1 FROM dbo.BoxMetadata WHERE BoxID = ? AND FieldID = ?)
                    UPDATE dbo.BoxMetadata
                    SET Value = ?
                    WHERE BoxID = ? AND FieldID = ?
                ELSE
                    INSERT INTO dbo.BoxMetadata (BoxID, FieldID, Value)
                    VALUES (?, ?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.setInt(2, fieldId);
            statement.setString(3, value);
            statement.setString(4, boxId);
            statement.setInt(5, fieldId);
            statement.setString(6, boxId);
            statement.setInt(7, fieldId);
            statement.setString(8, value);
            statement.executeUpdate();
        }
    }

    @Override
    public List<BoxMetadataEntry> getMetadataForBox(String boxId, List<MetadataField> fields) throws SQLException
    {
        String sql = """
                SELECT FieldID, Value
                FROM dbo.BoxMetadata
                WHERE BoxID = ?
                """;

        Map<Integer, String> storedValues = new HashMap<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);

            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    storedValues.put(rs.getInt("FieldID"), rs.getString("Value"));
                }
            }
        }

        List<BoxMetadataEntry> entries = new ArrayList<>();
        for (MetadataField field : fields)
        {
            String value = storedValues.getOrDefault(field.getId(), "");
            entries.add(new BoxMetadataEntry(0, boxId, field, value));
        }

        return entries;
    }

    @Override
    public void deleteMetadataForBox(String boxId) throws SQLException
    {
        String sql = "DELETE FROM dbo.BoxMetadata WHERE BoxID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.executeUpdate();
        }
    }
}