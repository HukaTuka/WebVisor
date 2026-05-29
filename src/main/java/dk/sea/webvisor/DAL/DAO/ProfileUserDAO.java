package dk.sea.webvisor.DAL.DAO;

import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.DAL.DBConnector.DBConnector;
import dk.sea.webvisor.DAL.Interface.ProfileUserInterface;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileUserDAO implements ProfileUserInterface {


    public ProfileUserDAO() throws IOException
    {
        DBConnector.getInstance();
    }

    @Override
    public void addProfileToUser(int userID, int profileID) throws SQLException
    {
        //Prevents double assignments
        String sql = """
               IF NOT EXISTS (
                               SELECT 1 FROM dbo.UserProfiles WHERE UserID = ? AND ProfileID = ?
                               )
               INSERT INTO dbo.UserProfiles (UserID, ProfileID) VALUES (?, ?)
               """;

        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userID);
            statement.setInt(2, profileID);
            statement.setInt(3, userID);
            statement.setInt(4, profileID);
            statement.executeUpdate();
        }
    }

    @Override
    public void removeProfileFromUser (int UserID, int ProfileID) throws SQLException
    {
        String sql = "DELETE FROM dbo.UserProfiles WHERE UserID = ? AND ProfileID = ?";

        try(Connection connection = DBConnector.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, UserID);
            statement.setInt(2, ProfileID);
            statement.executeUpdate();
        }
    }

    @Override
    public void removeAllProfilesFromUser(int userID) throws SQLException
    {
        String sql = "DELETE FROM dbo.UserProfiles WHERE UserID = ?";

        try(Connection connection = DBConnector.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userID);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Profile> getProfilesForUser(int userID) throws SQLException {
        String sql = """
            SELECT p.ID, p.Name, p.SplitOnBarcode, p.DefaultRotation, ISNULL(p.ClientID, 0) AS ClientID
            FROM dbo.Profiles p
            INNER JOIN dbo.UserProfiles up ON p.ID = up.ProfileID
            WHERE up.UserID = ?
            ORDER BY p.Name
        """;

        List<Profile> profiles = new ArrayList<>();
        try (Connection connection = DBConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, userID);
            try (ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    profiles.add(mapRow(rs));
                }
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
