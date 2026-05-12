package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.DAL.DAO.ClientsDAO;
import dk.sea.webvisor.DAL.Interface.ClientsInterface;

// Java Imports
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClientService
{
    private final ClientsInterface clientsDAO;

    public ClientService() throws IOException
    {
        this.clientsDAO = new ClientsDAO();
    }

    public List<Client> getAllClients() throws SQLException
    {
        return clientsDAO.getAllClients();
    }

    public Client createClient(String name) throws SQLException
    {
        String cleaned = validateName(name);
        Optional<Client> existing = clientsDAO.getClientByName(cleaned);
        if (existing.isPresent())
        {
            throw new IllegalArgumentException("Client already exists.");
        }
        return clientsDAO.createClient(cleaned);
    }

    public void updateClient(int clientId, String name) throws SQLException
    {
        if (clientId <= 0)
        {
            throw new IllegalArgumentException("Select a client from the table first.");
        }

        String cleaned = validateName(name);
        Optional<Client> existing = clientsDAO.getClientByName(cleaned);
        if (existing.isPresent() && existing.get().getId() != clientId)
        {
            throw new IllegalArgumentException("Client name is already in use.");
        }

        clientsDAO.updateClient(new Client(clientId, cleaned));
    }

    public void deleteClient(int clientId) throws SQLException
    {
        if (clientId <= 0)
        {
            throw new IllegalArgumentException("Select a client from the table first.");
        }

        List<String> assignedBoxes = clientsDAO.getBoxIdsAssignedToClient(clientId);
        if (!assignedBoxes.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Client cannot be deleted while boxes are assigned: " + String.join(", ", assignedBoxes));
        }

        clientsDAO.deleteClient(clientId);
    }

    public List<String> getBoxIdsAssignedToClient(int clientId) throws SQLException
    {
        return clientsDAO.getBoxIdsAssignedToClient(clientId);
    }

    private String validateName(String name)
    {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isBlank())
        {
            throw new IllegalArgumentException("Client name must not be empty.");
        }
        if (cleaned.length() > 150)
        {
            throw new IllegalArgumentException("Client name must not exceed 150 characters.");
        }
        return cleaned;
    }
}
