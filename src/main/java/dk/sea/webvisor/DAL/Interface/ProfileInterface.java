package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.Profile;

// Java Imports
import java.sql.SQLException;
import java.util.List;

public interface ProfileInterface {

    Profile createProfile(Profile profile) throws SQLException;
    void updateProfile(Profile profile) throws SQLException;
    void deleteProfile(int profileId) throws SQLException;
    List<Profile> getAllProfiles() throws SQLException;
    List<Integer> getUserIdsAssignedToProfile(int profileId) throws SQLException;
    List<String> getUsernamesAssignedToProfile(int profileId) throws SQLException;
}
