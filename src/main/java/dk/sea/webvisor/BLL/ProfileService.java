package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.DAL.DAO.ProfileDAO;
import dk.sea.webvisor.DAL.Interface.ProfileInterface;

// Java Imports
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ProfileService
{
    private final ProfileInterface profileDAO;

    public ProfileService() throws IOException
    {
        this.profileDAO = new ProfileDAO();
    }

    /**
     * Validates the supplied fields and persists a new profile.
     *
     * @param name            Display name for the profile (must not be blank).
     * @param splitOnBarcode  Whether a barcode page triggers a document split.
     * @param rotationDegrees Default page rotation in degrees (any integer,
     *                        normalised to [0, 359] internally).
     * @return the saved {@link Profile} with its database-assigned ID.
     * @throws IllegalArgumentException if validation fails.
     * @throws SQLException             if the database operation fails.
     */
    public Profile createProfile(String name, boolean splitOnBarcode, int rotationDegrees)
            throws SQLException
    {
        validateName(name);
        int normalised = Profile.normaliseRotation(rotationDegrees);
        Profile profile = new Profile(0, name.trim(), splitOnBarcode, normalised);
        return profileDAO.createProfile(profile);
    }

    /**
     * Updates an existing profile identified by {@code profileId}.
     *
     * @throws IllegalArgumentException if validation fails or the ID is invalid.
     * @throws SQLException             if the database operation fails.
     */
    public void updateProfile(int profileId, String name, boolean splitOnBarcode, int rotationDegrees)
            throws SQLException
    {
        if (profileId <= 0)
        {
            throw new IllegalArgumentException("Select a profile from the table first.");
        }

        validateName(name);
        int normalised = Profile.normaliseRotation(rotationDegrees);
        Profile profile = new Profile(profileId, name.trim(), splitOnBarcode, normalised);
        profileDAO.updateProfile(profile);
    }

    /**
     * Deletes the profile with the given ID.
     *
     * The caller is responsible for first consulting
     * {@link #getUsernamesAssignedToProfile(int)} and presenting a warning to
     * the user when the list is non-empty. This method performs the deletion
     * regardless -- the warning is purely informational.
     *
     * @throws IllegalArgumentException if the ID is invalid.
     * @throws SQLException             if the database operation fails.
     */
    public void deleteProfile(int profileId) throws SQLException
    {
        if (profileId <= 0)
        {
            throw new IllegalArgumentException("Select a profile from the table first.");
        }

        profileDAO.deleteProfile(profileId);
    }

    /** Returns all profiles ordered by name. */
    public List<Profile> getAllProfiles() throws SQLException
    {
        return profileDAO.getAllProfiles();
    }

    /**
     * Returns the usernames of every user assigned to the given profile.
     * An empty list means no users are affected by deletion.
     */
    public List<String> getUsernamesAssignedToProfile(int profileId) throws SQLException
    {
        return profileDAO.getUsernamesAssignedToProfile(profileId);
    }

    private void validateName(String name)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Profile name must not be empty.");
        }

        if (name.trim().length() > 100)
        {
            throw new IllegalArgumentException("Profile name must not exceed 100 characters.");
        }
    }
}