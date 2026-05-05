package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.FilesInterface;

// Java Imports
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class FilesDAO implements FilesInterface
{
    public FilesDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public int createFile(String boxId, Integer documentId, Files page) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Files (BoxID, DocumentID, PageNumber, IsBarcode, RotationDegrees, ImageData)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, boxId);
            if (documentId == null)
            {
                statement.setNull(2, Types.INTEGER);
            }
            else
            {
                statement.setInt(2, documentId);
            }
            statement.setInt(3, page.getPageNumber());
            statement.setBoolean(4, page.isBarcode());
            statement.setInt(5, page.getRotationDegrees());
            statement.setBytes(6, toPngBytes(page));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Could not read generated file ID.");
    }

    @Override
    public List<Files> getFilesByBox(String boxId) throws SQLException
    {
        String sql = """
                SELECT ID, PageNumber, IsBarcode, RotationDegrees
                FROM dbo.Files
                WHERE BoxID = ?
                ORDER BY PageNumber, ID
                """;

        List<Files> pages = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);

            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    pages.add(createPageMetadataFromResultSet(rs));
                }
            }
        }

        return pages;
    }

    @Override
    public List<Files> getFilesByDocument(int documentId) throws SQLException
    {
        String sql = """
                SELECT ID, PageNumber, IsBarcode, RotationDegrees
                FROM dbo.Files
                WHERE DocumentID = ?
                ORDER BY PageNumber, ID
                """;

        List<Files> pages = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, documentId);

            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    pages.add(createPageMetadataFromResultSet(rs));
                }
            }
        }

        return pages;
    }

    @Override
    public BufferedImage getFileImageById(int fileId) throws SQLException
    {
        String sql = """
                SELECT ImageData
                FROM dbo.Files
                WHERE ID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, fileId);

            try (ResultSet rs = statement.executeQuery())
            {
                if (!rs.next())
                {
                    throw new SQLException("File not found: " + fileId);
                }

                byte[] bytes = rs.getBytes("ImageData");
                if (bytes == null || bytes.length == 0)
                {
                    throw new SQLException("File " + fileId + " has no image data.");
                }

                try
                {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image == null)
                    {
                        throw new SQLException("Unsupported image data format for file " + fileId + ".");
                    }
                    return image;
                }
                catch (IOException e)
                {
                    throw new SQLException("Could not decode image data for file " + fileId + ".", e);
                }
            }
        }
    }

    @Override
    public void deleteFile(int fileId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Files WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, fileId);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteFilesByBox(String boxId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Files WHERE BoxID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, boxId);
            statement.executeUpdate();
        }
    }

    private Files createPageMetadataFromResultSet(ResultSet rs) throws SQLException
    {
        return new Files(
                rs.getInt("ID"),
                rs.getInt("PageNumber"),
                null,
                rs.getBoolean("IsBarcode"),
                rs.getInt("RotationDegrees")
        );
    }

    private byte[] toPngBytes(Files page) throws SQLException
    {
        if (page.getImage() == null)
        {
            throw new SQLException("Cannot save file " + page.getReferenceId() + " without image data.");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            if (!ImageIO.write(page.getImage(), "png", out))
            {
                throw new SQLException("Could not encode scanned page image as PNG.");
            }
            return out.toByteArray();
        }
        catch (IOException e)
        {
            throw new SQLException("Could not encode scanned page image.", e);
        }
    }

}
