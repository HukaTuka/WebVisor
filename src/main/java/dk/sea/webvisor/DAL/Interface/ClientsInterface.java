package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.Client;

// Java Imports
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ClientsInterface
{
    List<Client> getAllClients() throws SQLException;
    Optional<Client> getClientByName(String name) throws SQLException;
    Client createClient(String name) throws SQLException;
    void updateClient(Client client) throws SQLException;
    void deleteClient(int clientId) throws SQLException;
    List<String> getBoxIdsAssignedToClient(int clientId) throws SQLException;
}
