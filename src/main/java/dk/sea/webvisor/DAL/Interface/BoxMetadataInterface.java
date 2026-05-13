package dk.sea.webvisor.DAL.Interface;

import dk.sea.webvisor.BE.BoxMetadataEntry;
import dk.sea.webvisor.BE.MetadataField;

import java.sql.SQLException;
import java.util.List;

public interface BoxMetadataInterface
{
    void saveMetadata(String boxId, int fieldId, String value) throws SQLException;
    List<BoxMetadataEntry> getMetadataForBox(String boxId, List<MetadataField> fields) throws SQLException;
    void deleteMetadataForBox(String boxId) throws SQLException;
}