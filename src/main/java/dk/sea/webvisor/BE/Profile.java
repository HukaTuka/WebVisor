package dk.sea.webvisor.BE;

public class Profile {
    private int id;
    private String name;
    private boolean splitOnBarcode;
    private int defaultRotation;

    public Profile(int id, String name, boolean splitOnBarcode, int defaultRotation) {
        this.id = id;
        this.name = name;
        this.splitOnBarcode = splitOnBarcode;
        this.defaultRotation = normaliseRotation(defaultRotation);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        name = name;
    }

    public boolean isSplitOnBarcode() {
        return splitOnBarcode;
    }

    public void setSplitOnBarcode(boolean splitOnBarcode) {
        this.splitOnBarcode = splitOnBarcode;
    }

    public int getDefaultRotation() {
        return defaultRotation;
    }

    public void setDefaultRotation(int defaultRotation) {
        this.defaultRotation = defaultRotation;
    }

    public static int normaliseRotation(int degrees){
        return ((degrees % 360) + 360) % 360;
    }

    @Override
    public String toString()
    {
        return name + "  (rotation: " + defaultRotation
                + "°, split on barcode: " + (splitOnBarcode ? "yes" : "no") + ")";
    }
}
