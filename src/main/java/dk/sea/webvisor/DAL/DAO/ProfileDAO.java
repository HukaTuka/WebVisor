package dk.sea.webvisor.DAL.DAO;

// Project Imports
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.ProfileInterface;

// Java Imports
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDAO implements ProfileInterface
{
    public ProfileDAO() throws IOException
    {
        DBConnector.getInstance();
    }


    @Override
    public Profile createProfile(Profile profile) throws SQLException
    {
        String sql = """
                INSERT INTO dbo.Profiles (Name, SplitOnBarcode, DefaultRotation, ClientID)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, profile.getName());
            statement.setBoolean(2, profile.isSplitOnBarcode());
            statement.setInt(3, profile.getDefaultRotation());
            if (profile.getClientId() > 0)
                statement.setInt(4, profile.getClientId());
            else
                statement.setNull(4, Types.INTEGER);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys())
            {
                if (keys.next())
                {
                    int generatedId = keys.getInt(1);
                    return new Profile(
                            generatedId,
                            profile.getName(),
                            profile.isSplitOnBarcode(),
                            profile.getDefaultRotation(),
                            profile.getClientId()
                    );
                }
            }
        }

        throw new SQLException("Profile was inserted but no generated key was returned.");
    }

    @Override
    public void updateProfile(Profile profile) throws SQLException
    {
        String sql = """
                UPDATE dbo.Profiles
                SET Name            = ?,
                    SplitOnBarcode  = ?,
                    DefaultRotation = ?,
                    ClientID = ?
                WHERE ID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, profile.getName());
            statement.setBoolean(2, profile.isSplitOnBarcode());
            statement.setInt(3, profile.getDefaultRotation());
            if (profile.getClientId() > 0)
                statement.setInt(4, profile.getClientId());
            else
                statement.setNull(4, Types.INTEGER);
            statement.setInt(5, profile.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteProfile(int profileId) throws SQLException
    {
        String sql = "UPDATE dbo.Profiles SET IsDeleted = 1 WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, profileId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Profile> getAllProfiles() throws SQLException
    {
        String sql = """
            SELECT ID, Name, SplitOnBarcode, DefaultRotation, ISNULL(ClientID, 0) AS ClientID
            FROM dbo.Profiles
            WHERE IsDeleted = 0
            ORDER BY Name
            """;

        List<Profile> profiles = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery())
        {
            while (rs.next())
            {
                profiles.add(mapRow(rs));
            }
        }

        return profiles;
    }

    @Override
    public List<Integer> getUserIdsAssignedToProfile(int profileId) throws SQLException
    {
        String sql = """
            SELECT up.UserID
            FROM dbo.UserProfiles up
            INNER JOIN dbo.Users u ON u.ID = up.UserID
            WHERE up.ProfileID = ? AND u.IsDeleted = 0
            """;

        List<Integer> ids = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, profileId);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    ids.add(rs.getInt("UserID"));
                }
            }
        }

        return ids;
    }

    @Override
    public List<String> getUsernamesAssignedToProfile(int profileId) throws SQLException
    {
        String sql = """
            SELECT u.Username
            FROM dbo.Users u
            INNER JOIN dbo.UserProfiles up ON u.ID = up.UserID
            WHERE up.ProfileID = ? AND u.IsDeleted = 0
            ORDER BY u.Username
            """;

        List<String> names = new ArrayList<>();

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, profileId);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    names.add(rs.getString("Username"));
                }
            }
        }

        return names;
    }

    @Override
    public List<Profile> getProfilesByClient(int clientId) throws SQLException
    {
        String sql = """
            SELECT ID, Name, SplitOnBarcode, DefaultRotation, ISNULL(ClientID, 0) AS ClientID
            FROM dbo.Profiles
            WHERE ClientID = ? AND IsDeleted = 0
            ORDER BY Name
            """;

        List<Profile> profiles = new ArrayList<>();
        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, clientId);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next()) profiles.add(mapRow(rs));
            }
        }
        return profiles;
    }

    private Profile mapRow(ResultSet rs) throws SQLException
    {
        return new Profile(
                rs.getInt("ID"),
                rs.getString("Name"),
                rs.getBoolean("SplitOnBarcode"),
                rs.getInt("DefaultRotation"),
                rs.getInt("ClientID")
        );
    }
}