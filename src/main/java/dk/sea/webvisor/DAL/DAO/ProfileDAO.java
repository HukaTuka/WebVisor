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
                INSERT INTO dbo.Profiles (Name, SplitOnBarcode, DefaultRotation)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            statement.setString(1, profile.getName());
            statement.setBoolean(2, profile.isSplitOnBarcode());
            statement.setInt(3, profile.getDefaultRotation());
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
                            profile.getDefaultRotation()
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
                    DefaultRotation = ?
                WHERE ID = ?
                """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, profile.getName());
            statement.setBoolean(2, profile.isSplitOnBarcode());
            statement.setInt(3, profile.getDefaultRotation());
            statement.setInt(4, profile.getId());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteProfile(int profileId) throws SQLException
    {
        // Remove junction rows first to avoid a foreign-key violation
        String deleteAssignments = "DELETE FROM dbo.UserProfiles WHERE ProfileID = ?";
        String deleteProfile     = "DELETE FROM dbo.Profiles WHERE ID = ?";

        try (Connection connection = DBConnector.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                try (PreparedStatement s1 = connection.prepareStatement(deleteAssignments))
                {
                    s1.setInt(1, profileId);
                    s1.executeUpdate();
                }

                try (PreparedStatement s2 = connection.prepareStatement(deleteProfile))
                {
                    s2.setInt(1, profileId);
                    s2.executeUpdate();
                }

                connection.commit();
            }
            catch (SQLException e)
            {
                connection.rollback();
                throw e;
            }
            finally
            {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public List<Profile> getAllProfiles() throws SQLException
    {
        String sql = """
                SELECT ID, Name, SplitOnBarcode, DefaultRotation
                FROM dbo.Profiles
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
        String sql = "SELECT UserID FROM dbo.UserProfiles WHERE ProfileID = ?";
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
                WHERE up.ProfileID = ?
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


    private Profile mapRow(ResultSet rs) throws SQLException
    {
        return new Profile(
                rs.getInt("ID"),
                rs.getString("Name"),
                rs.getBoolean("SplitOnBarcode"),
                rs.getInt("DefaultRotation")
        );
    }
}