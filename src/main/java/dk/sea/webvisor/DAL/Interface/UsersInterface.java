package dk.sea.webvisor.DAL.Interface;

// Project Imports
import dk.sea.webvisor.BE.User;

// Java Imports
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UsersInterface
{
    Optional<User> getUserByUsername(String username) throws SQLException;
    List<User> getAllUsers() throws SQLException;
    User createUser(User user) throws SQLException;
    void updateUser(User user) throws SQLException;
    void deleteUser(int userId) throws SQLException;
}
