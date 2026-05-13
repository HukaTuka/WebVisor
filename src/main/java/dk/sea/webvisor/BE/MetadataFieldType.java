package dk.sea.webvisor.BE;

public enum MetadataFieldType
{
    TEXT("Text"),
    NUMBER("Number"),
    DATE("Date");

    private final String displayName;

    MetadataFieldType(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public static MetadataFieldType fromString(String value)
    {
        if (value == null)
        {
            return TEXT;
        }

        for (MetadataFieldType type : values())
        {
            if (type.name().equalsIgnoreCase(value.trim()))
            {
                return type;
            }
        }

        return TEXT;
    }
}