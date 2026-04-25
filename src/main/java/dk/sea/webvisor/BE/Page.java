package dk.sea.webvisor.BE;

public class Page {
    private int pageId;
    private int docId;
    private int pageOrder;      // 1-based display order within the document
    private int rotationDegrees;// 0, 90, 180, 270
    private byte[] imageData;   // raw TIFF bytes

    public Page() {
        this.rotationDegrees = 0;
    }

    public Page(int pageId, int docId, int pageOrder, int rotationDegrees, byte[] imageData) {
        this.pageId          = pageId;
        this.docId           = docId;
        this.pageOrder       = pageOrder;
        this.rotationDegrees = rotationDegrees;
        this.imageData       = imageData;
    }

    public int getPageId() { return pageId; }
    public void setPageId(int id) { this.pageId = id; }
    public int getDocId() { return docId; }
    public void setDocId(int id) { this.docId = id; }
    public int getPageOrder() { return pageOrder; }
    public void setPageOrder(int order) { this.pageOrder = order; }
    public int getRotationDegrees() { return rotationDegrees; }
    public void setRotationDegrees(int deg) { this.rotationDegrees = deg; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] data) { this.imageData = data; }

    /** Rotate clockwise by 90 degrees. */
    public void rotateClockwise() {
        rotationDegrees = (rotationDegrees + 90) % 360;
    }

    /** Rotate counter-clockwise by 90 degrees. */
    public void rotateCounterClockwise() {
        rotationDegrees = (rotationDegrees + 270) % 360;
    }

    @Override
    public String toString() {
        return "Page " + pageOrder + " (rot: " + rotationDegrees + "°)";
    }
}
