package dk.sea.webvisor.BE;

public class BoxMetadataEntry
{
    private final int id;
    private final String boxId;
    private final MetadataField field;
    private String value;

    public BoxMetadataEntry(int id, String boxId, MetadataField field, String value)
    {
        this.id    = id;
        this.boxId = boxId;
        this.field = field;
        this.value = value == null ? "" : value;
    }

    public int getId()
    {
        return id;
    }

    public String getBoxId()
    {
        return boxId;
    }

    public MetadataField getField()
    {
        return field;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value == null ? "" : value;
    }
}