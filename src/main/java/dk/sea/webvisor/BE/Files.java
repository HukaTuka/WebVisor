package dk.sea.webvisor.BE;

import java.awt.image.BufferedImage;

/**
 * Represents a single page fetched from the TIFF scanning API.
 * Tracks the page number within the current session, the raw image,
 * whether the API returned a barcode (document-split signal), and any
 * rotation the operator has applied before saving.
 */
public class Files
{
    private final int id;
    private final int pageNumber;
    private BufferedImage image;
    private final boolean isBarcode;
    private int rotationDegrees; // 0 | 90 | 180 | 270

    public Files(int pageNumber, BufferedImage image, boolean isBarcode)
    {
        this(0, pageNumber, image, isBarcode, 0);
    }

    public Files(int id, int pageNumber, BufferedImage image, boolean isBarcode, int rotationDegrees)
    {
        this.id = id;
        this.pageNumber = pageNumber;
        this.image = image;
        this.isBarcode = isBarcode;
        this.rotationDegrees = normalizeRotation(rotationDegrees);
    }


    public int getId()              { return id; }
    public int getPageNumber()      { return pageNumber; }
    public BufferedImage getImage() { return image; }
    public boolean isBarcode()      { return isBarcode; }
    public int getRotationDegrees() { return rotationDegrees; }
    public void setImage(BufferedImage image) { this.image = image; }


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

    public String getReferenceId() {
    return String.format("Scan-%03d", pageNumber);}

    private int normalizeRotation(int rotationDegrees)
    {
        int normalized = rotationDegrees % 360;
        if (normalized < 0)
        {
            normalized += 360;
        }
        return normalized;
    }

    @Override
    public String toString()
    {
        return isBarcode
                ? getReferenceId() + " [BARCODE]"
                : getReferenceId();
    }
}
