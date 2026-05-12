package dk.sea.webvisor.BE;

public class Archive
{
    private final int id;
    private final int clientId;
    private final String name;
    private final String clientName;

    public Archive(int id, int clientId, String name, String clientName)
    {
        this.id = id;
        this.clientId = clientId;
        this.name = name == null ? "" : name;
        this.clientName = clientName == null ? "" : clientName;
    }

    public int getId()
    {
        return id;
    }

    public int getClientId()
    {
        return clientId;
    }

    public String getName()
    {
        return name;
    }

    public String getClientName()
    {
        return clientName;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
