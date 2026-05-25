package dk.sea.webvisor.BE;

public enum DocumentStatus
{
    IN_PROGRESS("In Progress"),
    WAITING_FOR_QA("Waiting for QA"),
    QA_COMPLETED("QA Completed"),
    REJECTED("Rejected");

    private final String displayName;

    DocumentStatus(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public static DocumentStatus fromString(String value)
    {
        if (value == null)
        {
            return IN_PROGRESS;
        }

        for (DocumentStatus status : values())
        {
            if (status.name().equalsIgnoreCase(value.trim()))
            {
                return status;
            }
        }

        return IN_PROGRESS;
    }
}