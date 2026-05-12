package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.ClientsInterface;

// Java Imports
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientsDAO implements ClientsInterface
{
    public ClientsDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public List<Client> getAllClients() throws SQLException
    {
        String sql = """
                SELECT ID, Name
                FROM dbo.Clients
                ORDER BY Name
                """;

        List<Client> clients = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                clients.add(new Client(
                        rs.getInt("ID"),
                        rs.getString("Name")
                ));
            }
        }

        return clients;
    }

    @Override
    public Optional<Client> getClientByName(String name) throws SQLException
    {
        String sql = """
                SELECT TOP 1 ID, Name
                FROM dbo.Clients
                WHERE Name = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery())
            {
                if (!rs.next())
                {
                    return Optional.empty();
                }

                return Optional.of(new Client(
                        rs.getInt("ID"),
                        rs.getString("Name")
                ));
            }
        }
    }

    @Override
    public Client createClient(String name) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Clients (Name)
                VALUES (?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, name);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    return new Client(keys.getInt(1), name);
                }
            }
        }

        Optional<Client> existing = getClientByName(name);
        if (existing.isPresent())
        {
            return existing.get();
        }

        throw new SQLException("Could not create client: " + name);
    }

    @Override
    public void updateClient(Client client) throws SQLException
    {
        String sql = """
                UPDATE dbo.Clients
                SET Name = ?
                WHERE ID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, client.getName());
            statement.setInt(2, client.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteClient(int clientId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Clients WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, clientId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<String> getBoxIdsAssignedToClient(int clientId) throws SQLException
    {
        String sql = """
                SELECT b.BoxID
                FROM dbo.Boxes b
                WHERE b.ClientID = ?
                ORDER BY b.BoxID
                """;

        List<String> boxIds = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    boxIds.add(rs.getString("BoxID"));
                }
            }
        }

        return boxIds;
    }
}
