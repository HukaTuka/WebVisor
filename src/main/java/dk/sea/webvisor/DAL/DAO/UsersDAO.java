package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.UserAdmin;
import dk.sea.webvisor.BE.UserRole;
import dk.sea.webvisor.BE.UserScanner;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.UsersInterface;

// Java Imports
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsersDAO implements UsersInterface
{
    public UsersDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public Optional<User> getUserByUsername(String username) throws SQLException
    {
        String sql = """
                SELECT TOP 1 ID, FirstName, LastName, Username, Password, Role
                FROM dbo.Users
                WHERE Username = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery())
            {
                if (!rs.next())
                {
                    return Optional.empty();
                }

                int id = rs.getInt("ID");
                String firstName = rs.getString("FirstName");
                String lastName = rs.getString("LastName");
                String dbUsername = rs.getString("Username");
                String password = rs.getString("Password");
                UserRole role = mapRole(rs.getString("Role"));

                User user = role == UserRole.UserAdmin
                        ? new UserAdmin(id, firstName, lastName, dbUsername, password, role)
                        : new UserScanner(id, firstName, lastName, dbUsername, password, role);

                return Optional.of(user);
            }
        }
    }

    @Override
    public List<User> getAllUsers() throws SQLException
    {
        String sql = """
                SELECT ID, FirstName, LastName, Username, Password, Role
                FROM dbo.Users
                ORDER BY Username
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                users.add(createUserFromResultSet(rs));
            }
        }

        return users;
    }

    @Override
    public User createUser(User user) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Users (ID, FirstName, LastName, Username, Password, Role, [Timestamp])
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        int newId = getNextUserId();
        int nowTimestamp = (int) (System.currentTimeMillis() / 1000L);

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, newId);
            statement.setString(2, user.getFirstName());
            statement.setString(3, user.getLastName());
            statement.setString(4, user.getUsername());
            statement.setString(5, user.getPassword());
            statement.setString(6, toDatabaseRole(user.getRole()));
            statement.setInt(7, nowTimestamp);
            statement.executeUpdate();
        }

        return createUserObject(
                newId,
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getPassword(),
                user.getRole()
        );
    }

    @Override
    public void updateUser(User user) throws SQLException
    {
        String sql = """
                UPDATE dbo.Users
                SET FirstName = ?, LastName = ?, Username = ?, Password = ?, Role = ?, [Timestamp] = ?
                WHERE ID = ?
                """;

        int nowTimestamp = (int) (System.currentTimeMillis() / 1000L);

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, user.getFirstName());
            statement.setString(2, user.getLastName());
            statement.setString(3, user.getUsername());
            statement.setString(4, user.getPassword());
            statement.setString(5, toDatabaseRole(user.getRole()));
            statement.setInt(6, nowTimestamp);
            statement.setInt(7, user.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteUser(int userId) throws SQLException
    {
        String sql = "DELETE FROM dbo.Users WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    private int getNextUserId() throws SQLException
    {
        String sql = "SELECT ISNULL(MAX(ID), 0) + 1 AS NextId FROM dbo.Users";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            if (rs.next())
            {
                return rs.getInt("NextId");
            }
        }

        throw new SQLException("Could not generate new user ID.");
    }

    private User createUserFromResultSet(ResultSet rs) throws SQLException
    {
        int id = rs.getInt("ID");
        String firstName = rs.getString("FirstName");
        String lastName = rs.getString("LastName");
        String username = rs.getString("Username");
        String password = rs.getString("Password");
        UserRole role = mapRole(rs.getString("Role"));

        return createUserObject(id, firstName, lastName, username, password, role);
    }

    private User createUserObject(int id, String firstName, String lastName, String username, String password, UserRole role)
    {
        if (role == UserRole.UserAdmin)
        {
            return new UserAdmin(id, firstName, lastName, username, password, role);
        }
        return new UserScanner(id, firstName, lastName, username, password, role);
    }

    private String toDatabaseRole(UserRole role)
    {
        if (role == UserRole.UserAdmin)
        {
            return "Administrator";
        }
        return "Scanner";
    }

    private UserRole mapRole(String roleValue)
    {
        if (roleValue == null)
        {
            return UserRole.UserScanner;
        }

        String role = roleValue.trim().toLowerCase();
        if (role.equals("administrator") || role.equals("admin"))
        {
            return UserRole.UserAdmin;
        }

        return UserRole.UserScanner;
    }
}
