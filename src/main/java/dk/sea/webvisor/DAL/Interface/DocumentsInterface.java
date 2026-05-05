package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.Document;

// Java Imports
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface DocumentsInterface
{
    int createDocument(String boxId, int documentNumber) throws SQLException;
    Optional<Integer> getDocumentId(String boxId, int documentNumber) throws SQLException;
    List<Document> getDocumentsByBox(String boxId) throws SQLException;
    void deleteDocument(int documentId) throws SQLException;
    void deleteDocumentsByBox(String boxId) throws SQLException;
}
