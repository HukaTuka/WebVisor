package dk.sea.webvisor.BE;

public class MetadataField
{
    private int id;
    private String name;
    private MetadataFieldType fieldType;

    public MetadataField(int id, String name, MetadataFieldType fieldType)
    {
        this.id        = id;
        this.name      = name == null ? "" : name;
        this.fieldType = fieldType == null ? MetadataFieldType.TEXT : fieldType;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name == null ? "" : name;
    }

    public MetadataFieldType getFieldType()
    {
        return fieldType;
    }

    public void setFieldType(MetadataFieldType fieldType)
    {
        this.fieldType = fieldType == null ? MetadataFieldType.TEXT : fieldType;
    }

    @Override
    public String toString()
    {
        return name + " (" + fieldType.getDisplayName() + ")";
    }
}