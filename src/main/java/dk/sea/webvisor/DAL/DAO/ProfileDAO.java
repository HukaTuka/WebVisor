package dk.sea.webvisor.DAL.DAO;

import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.DAL.Interface.ProfileInterface;

import java.sql.SQLException;
import java.util.List;

public class ProfileDAO implements ProfileInterface {
    @Override
    public Profile createProfile(Profile profile) throws SQLException {
        return null;
    }

    @Override
    public void updateProfile(Profile profile) throws SQLException {

    }

    @Override
    public void deleteProfile(int profileId) throws SQLException {

    }

    @Override
    public List<Profile> getAllProfiles() throws SQLException {
        return List.of();
    }

    @Override
    public List<Integer> getUserIdsAssignedToProfile(int profileId) throws SQLException {
        return List.of();
    }

    @Override
    public List<String> getUsernamesAssignedToProfile(int profileId) throws SQLException {
        return List.of();
    }
}
