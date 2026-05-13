package dk.sea.webvisor.DAL.DAO;

import dk.sea.webvisor.BE.MetadataField;
import dk.sea.webvisor.BE.MetadataFieldType;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.MetadataFieldsInterface;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MetadataFieldsDAO implements MetadataFieldsInterface
{
    public MetadataFieldsDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public MetadataField createField(MetadataField field) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.MetadataFields (Name, FieldType)
                VALUES (?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, field.getName());
            statement.setString(2, field.getFieldType().name());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return new MetadataField(keys.getInt(1), field.getName(), field.getFieldType());
                }
            }
        }

        throw new SQLException("Could not read generated metadata field ID.");
    }

    @Override
    public void updateField(MetadataField field) throws SQLException
    {
        String sql = """
                UPDATE dbo.MetadataFields
                SET Name = ?, FieldType = ?
                WHERE ID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, field.getName());
            statement.setString(2, field.getFieldType().name());
            statement.setInt(3, field.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteField(int fieldId) throws SQLException
    {
        String sql = "DELETE FROM dbo.MetadataFields WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, fieldId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<MetadataField> getAllFields() throws SQLException
    {
        String sql = """
                SELECT ID, Name, FieldType
                FROM dbo.MetadataFields
                ORDER BY Name
                """;

        List<MetadataField> fields = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                fields.add(mapRow(rs));
            }
        }

        return fields;
    }

    private MetadataField mapRow(ResultSet rs) throws SQLException
    {
        return new MetadataField(
                rs.getInt("ID"),
                rs.getString("Name"),
                MetadataFieldType.fromString(rs.getString("FieldType"))
        );
    }
}