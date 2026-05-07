package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.Files;

// Java Imports
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.List;

public interface FilesInterface
{
    int createFile(String boxId, Integer documentId, Files page) throws SQLException;
    List<Files> getFilesByBox(String boxId) throws SQLException;
    List<Files> getFilesByDocument(int documentId) throws SQLException;
    void updatePageOrder(String boxId, List<Integer> orderedFileIds) throws SQLException;
    BufferedImage getFileImageById(int fileId) throws SQLException;
    void deleteFile(int fileId) throws SQLException;
    void deleteFilesByBox(String boxId) throws SQLException;
}
