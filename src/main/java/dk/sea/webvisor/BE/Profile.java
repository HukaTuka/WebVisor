package dk.sea.webvisor.BE;

public class Profile {
    private int id;
    private String name;
    private boolean splitOnBarcode;
    private int defaultRotation;
    private int clientId;

    public Profile(int id, String name, boolean splitOnBarcode, int defaultRotation, int clientId) {
        this.id = id;
        this.name = name;
        this.splitOnBarcode = splitOnBarcode;
        this.defaultRotation = normaliseRotation(defaultRotation);
        this.clientId = clientId;
    }

    //keeping old constructor as to not break anything
    public Profile(int id, String name, boolean splitOnBarcode, int defaultRotation) {
        this(id, name, splitOnBarcode, defaultRotation, 0);
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

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Profile other)) return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString()
    {
        return name + "  (rotation: " + defaultRotation + "\u00b0)";
    }
}