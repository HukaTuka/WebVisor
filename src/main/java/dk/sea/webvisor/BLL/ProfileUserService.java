package dk.sea.webvisor.BLL;

import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.DAL.DAO.ProfileUserDAO;
import dk.sea.webvisor.DAL.Interface.ProfileUserInterface;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ProfileUserService {
    private final ProfileUserInterface profileUserDao;

    public ProfileUserService() throws IOException
    {
        this.profileUserDao = new ProfileUserDAO();
    }

    //adds the profile to a user
    public void addProfileToUser(int userID, int profileID) throws SQLException
    {
        if (userID <= 0)
        {
            throw new IllegalArgumentException("Invalid user ID, User ID must be larger than 0");
        }
        if (profileID <= 0)
        {
            throw new IllegalArgumentException("Invalid profile ID, Profile ID must be larger than 0");
        }

        profileUserDao.addProfileToUser(userID, profileID);
    }

    //removes a specific profile from user
    public void removeProfileFromUser(int userID, int profileID) throws SQLException
    {
        if (userID <= 0)
        {
            throw new IllegalArgumentException("Invalid user ID, User ID must be larger than 0");
        }
        if (profileID <= 0)
        {
            throw new IllegalArgumentException("Invalid profile ID, Profile ID must be larger than 0");
        }

        profileUserDao.removeProfileFromUser(userID, profileID);
    }

    //removes ALL the profiles from a user
    public void removeAllProfilesFromUser(int userID)  throws SQLException
    {
        if (userID <= 0)
        {
            throw new IllegalArgumentException("Invalid user ID, User ID must be larger than 0");
        }

        profileUserDao.removeAllProfilesFromUser(userID);
    }

    //returns all the profiles assigned to user ordered by name
    public List<Profile> getProfilesForUser(int userId) throws SQLException
    {
        if (userId <= 0)
        {
            throw new IllegalArgumentException("Invalid user ID, User ID must be larger than 0");
        }

        return profileUserDao.getProfilesForUser(userId);
    }

    private void validateName(String name)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Invalid name, Name can not be null or blank");
        }

        if (name.trim().length() > 100)
        {
            throw new IllegalArgumentException("Profile name can not exceed 100 characters");
        }
    }
}
