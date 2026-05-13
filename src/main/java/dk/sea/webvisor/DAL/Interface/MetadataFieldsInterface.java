package dk.sea.webvisor.DAL.Interface;

import dk.sea.webvisor.BE.MetadataField;

import java.sql.SQLException;
import java.util.List;

public interface MetadataFieldsInterface
{
    MetadataField createField(MetadataField field) throws SQLException;
    void updateField(MetadataField field) throws SQLException;
    void deleteField(int fieldId) throws SQLException;
    List<MetadataField> getAllFields() throws SQLException;
}