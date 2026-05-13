package dk.sea.webvisor.BLL;

import dk.sea.webvisor.BE.MetadataField;
import dk.sea.webvisor.BE.MetadataFieldType;
import dk.sea.webvisor.DAL.DAO.MetadataFieldsDAO;
import dk.sea.webvisor.DAL.Interface.MetadataFieldsInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MetadataFieldService
{
    private final MetadataFieldsInterface metadataFieldsDAO;

    public MetadataFieldService() throws IOException
    {
        this.metadataFieldsDAO = new MetadataFieldsDAO();
    }

    public MetadataField createField(String name, MetadataFieldType fieldType) throws SQLException
    {
        validateName(name);
        MetadataField field = new MetadataField(0, name.trim(), fieldType);
        return metadataFieldsDAO.createField(field);
    }

    public void updateField(int fieldId, String name, MetadataFieldType fieldType) throws SQLException
    {
        if (fieldId <= 0)
        {
            throw new IllegalArgumentException("Select a field from the table first.");
        }

        validateName(name);
        MetadataField field = new MetadataField(fieldId, name.trim(), fieldType);
        metadataFieldsDAO.updateField(field);
    }

    public void deleteField(int fieldId) throws SQLException
    {
        if (fieldId <= 0)
        {
            throw new IllegalArgumentException("Select a field from the table first.");
        }

        metadataFieldsDAO.deleteField(fieldId);
    }

    public List<MetadataField> getAllFields() throws SQLException
    {
        return metadataFieldsDAO.getAllFields();
    }

    private void validateName(String name)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Field name must not be empty.");
        }

        if (name.trim().length() > 150)
        {
            throw new IllegalArgumentException("Field name must not exceed 150 characters.");
        }
    }
}