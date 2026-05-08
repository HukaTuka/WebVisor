package dk.sea.webvisor.DAL.Interface;

import dk.sea.webvisor.BE.Profile;

import java.sql.SQLException;
import java.util.List;

public interface ProfileUserInterface {
    void addProfileToUser(int UserID, int ProfileID) throws SQLException;
    void removeProfileFromUser(int UserID, int ProfileID) throws SQLException;
    void removeAllProfilesFromUser(int UserID) throws SQLException;
    List<Profile> getProfilesForUser(int userID) throws SQLException;
}
