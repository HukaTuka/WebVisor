package dk.sea.webvisor.BLL;

import dk.sea.webvisor.BE.BoxMetadataEntry;
import dk.sea.webvisor.BE.MetadataField;
import dk.sea.webvisor.BE.MetadataFieldType;
import dk.sea.webvisor.DAL.DAO.BoxMetadataDAO;
import dk.sea.webvisor.DAL.Interface.BoxMetadataInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class BoxMetadataService
{
    private final BoxMetadataInterface boxMetadataDAO;

    public BoxMetadataService() throws IOException
    {
        this.boxMetadataDAO = new BoxMetadataDAO();
    }

    public void saveMetadata(String boxId, int fieldId, MetadataFieldType fieldType, String value) throws SQLException
    {
        if (boxId == null || boxId.isBlank())
        {
            throw new IllegalArgumentException("Box ID must not be empty.");
        }

        if (fieldId <= 0)
        {
            throw new IllegalArgumentException("Invalid field ID.");
        }

        validateValueForType(value, fieldType);
        boxMetadataDAO.saveMetadata(boxId, fieldId, value == null ? "" : value.trim());
    }

    public List<BoxMetadataEntry> getMetadataForBox(String boxId, List<MetadataField> fields) throws SQLException
    {
        if (boxId == null || boxId.isBlank())
        {
            throw new IllegalArgumentException("Box ID must not be empty.");
        }

        return boxMetadataDAO.getMetadataForBox(boxId, fields);
    }

    public void deleteMetadataForBox(String boxId) throws SQLException
    {
        if (boxId == null || boxId.isBlank())
        {
            throw new IllegalArgumentException("Box ID must not be empty.");
        }

        boxMetadataDAO.deleteMetadataForBox(boxId);
    }

    private void validateValueForType(String value, MetadataFieldType fieldType)
    {
        if (value == null || value.isBlank())
        {
            return;
        }

        if (fieldType == MetadataFieldType.NUMBER)
        {
            try
            {
                Double.parseDouble(value.trim());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException("Value for a Number field must be a valid number.");
            }
        }

        if (fieldType == MetadataFieldType.DATE)
        {
            if (!value.trim().matches("\\d{4}-\\d{2}-\\d{2}"))
            {
                throw new IllegalArgumentException("Value for a Date field must use the format YYYY-MM-DD.");
            }
        }
    }
}