package dk.sea.webvisor.BE;

import java.awt.image.BufferedImage;

/**
 * Represents a single page fetched from the TIFF scanning API.
 * Tracks the page number within the current session, the raw image,
 * whether the API returned a barcode (document-split signal), and any
 * rotation the operator has applied before saving.
 */
public class ScannedPage
{
    private final int pageNumber;
    private final BufferedImage image;
    private final boolean isBarcode;
    private int rotationDegrees; // 0 | 90 | 180 | 270

    public ScannedPage(int pageNumber, BufferedImage image, boolean isBarcode)
    {
        this.pageNumber = pageNumber;
        this.image = image;
        this.isBarcode = isBarcode;
        this.rotationDegrees = 0;
    }


    public int getPageNumber()      { return pageNumber; }
    public BufferedImage getImage() { return image; }
    public boolean isBarcode()      { return isBarcode; }
    public int getRotationDegrees() { return rotationDegrees; }


    /** Rotate 90° clockwise. */
    public void rotateRight()
    {
        rotationDegrees = (rotationDegrees + 90) % 360;
    }

    /** Rotate 90° counter-clockwise. */
    public void rotateLeft()
    {
        rotationDegrees = (rotationDegrees + 270) % 360;
    }

    @Override
    public String toString()
    {
        return isBarcode
                ? "Page " + pageNumber + " [BARCODE]"
                : "Page " + pageNumber;
    }
}
