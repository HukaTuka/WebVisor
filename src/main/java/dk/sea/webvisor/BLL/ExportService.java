package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;

// Java Imports
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExportService
{
    public int exportSinglePage(List<Document> documents, File outputDirectory, String folderName)
            throws IOException
    {
        File exportRoot = createDirectory(outputDirectory, folderName);
        int  fileCount  = 0;

        for (Document document : documents)
        {
            List<Files> contentPages = getContentPages(document);
            if (contentPages.isEmpty()) { continue; }

            File documentDirectory = createDirectory(exportRoot, "Document_" + document.getDocumentNumber());

            for (Files page : contentPages)
            {
                BufferedImage rotated = applyRotation(page.getImage(), page.getRotationDegrees());
                File outputFile = new File(documentDirectory, page.getReferenceId() + ".tiff");
                writeSinglePageTiff(rotated, outputFile);
                fileCount++;
            }
        }

        return fileCount;
    }

    public int exportMultiPage(List<Document> documents, File outputDirectory, String folderName)
            throws IOException
    {
        File exportRoot    = createDirectory(outputDirectory, folderName);
        int  documentCount = 0;

        for (Document document : documents)
        {
            List<Files> contentPages = getContentPages(document);
            if (contentPages.isEmpty()) { continue; }

            List<BufferedImage> rotatedImages = new ArrayList<>();
            for (Files page : contentPages)
            {
                rotatedImages.add(applyRotation(page.getImage(), page.getRotationDegrees()));
            }

            File outputFile = new File(exportRoot, "Document_" + document.getDocumentNumber() + ".tiff");
            writeMultiPageTiff(rotatedImages, outputFile);
            documentCount++;
        }

        return documentCount;
    }

    private File createDirectory(File parent, String name) throws IOException
    {
        File directory = new File(parent, name);
        if (!directory.exists() && !directory.mkdirs())
        {
            throw new IOException("Could not create export directory: " + directory.getAbsolutePath());
        }
        return directory;
    }

    private List<Files> getContentPages(Document document)
    {
        List<Files> contentPages = new ArrayList<>();
        for (Files page : document.getPages())
        {
            if (!page.isBarcode() && page.getImage() != null)
            {
                contentPages.add(page);
            }
        }
        return contentPages;
    }

    private void writeSinglePageTiff(BufferedImage image, File outputFile) throws IOException
    {
        if (!ImageIO.write(image, "TIFF", outputFile))
        {
            throw new IOException("No TIFF writer is registered for: " + outputFile.getName());
        }
    }

    private void writeMultiPageTiff(List<BufferedImage> images, File outputFile) throws IOException
    {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext())
        {
            throw new IOException("No TIFF image writer is available on this JVM.");
        }

        ImageWriter writer = writers.next();

        if (!writer.canWriteSequence())
        {
            writer.dispose();
            throw new IOException("The available TIFF writer does not support multi-page output.");
        }

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile))
        {
            writer.setOutput(outputStream);
            writer.prepareWriteSequence(null);

            for (BufferedImage image : images)
            {
                writer.writeToSequence(new IIOImage(image, null, null), null);
            }

            writer.endWriteSequence();
        }
        finally
        {
            writer.dispose();
        }
    }

    private BufferedImage applyRotation(BufferedImage source, int degrees)
    {
        if (degrees == 0 || source == null) { return source; }

        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth  = (int) Math.floor(source.getWidth()  * cos + source.getHeight() * sin);
        int newHeight = (int) Math.floor(source.getWidth()  * sin + source.getHeight() * cos);

        int imageType = source.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : source.getType();
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, imageType);

        AffineTransform transform = new AffineTransform();
        transform.translate(
                (newWidth  - source.getWidth())  / 2.0,
                (newHeight - source.getHeight()) / 2.0);
        transform.rotate(radians, source.getWidth() / 2.0, source.getHeight() / 2.0);

        new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR).filter(source, rotated);
        return rotated;
    }
}