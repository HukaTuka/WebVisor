package dk.sea.webvisor.BLL;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.UserAdmin;
import dk.sea.webvisor.BE.UserRole;
import dk.sea.webvisor.BE.UserScanner;
import dk.sea.webvisor.BLL.Util.PasswordHasher;
import dk.sea.webvisor.DAL.DAO.UsersDAO;
import dk.sea.webvisor.DAL.Interface.UsersInterface;

// Java Imports
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserService
{
    private final UsersInterface usersDAO;

    public UserService() throws IOException
    {
        this.usersDAO = new UsersDAO();
    }

    public User login(String username, String password) throws SQLException
    {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must be filled out.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must be filled out.");
        }

        Optional<User> optionalUser = usersDAO.getUserByUsername(username.trim());
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Wrong username or password.");
        }

        User user = optionalUser.get();
        if (!PasswordHasher.verifyPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Wrong username or password.");
        }

        // Update last login timestamp
        user.setLastLogin(LocalDateTime.now());
        usersDAO.updateUser(user);

        return user;
    }

    public List<User> getAllUsers() throws SQLException
    {
        return usersDAO.getAllUsers();
    }

    public User createUser(String username, String plainPassword, UserRole role, LocalDateTime lastLogin) throws SQLException
    {
        validateUserInput(username, plainPassword, role);

        String hashedPassword = PasswordHasher.hashPassword(plainPassword.trim());
        User user = createUserObject(0, username, hashedPassword, role, lastLogin);
        return usersDAO.createUser(user);
    }

    public void updateUser(int userId, String username, String plainPassword, UserRole role, LocalDateTime lastLogin) throws SQLException
    {
        if (userId <= 0)
        {
            throw new IllegalArgumentException("Please choose a valid user.");
        }

        validateUserInput(username, plainPassword, role);

        String hashedPassword = PasswordHasher.hashPassword(plainPassword.trim());
        User user = createUserObject(userId, username, hashedPassword, role, lastLogin);
        usersDAO.updateUser(user);
    }

    public void deleteUser(int userId) throws SQLException
    {
        if (userId <= 0)
        {
            throw new IllegalArgumentException("Please choose a valid user.");
        }

        usersDAO.deleteUser(userId);
    }

    private void validateUserInput(String username, String plainPassword, UserRole role)
    {

        if (username == null || username.isBlank())
        {
            throw new IllegalArgumentException("Username must be filled out.");
        }

        if (plainPassword == null || plainPassword.isBlank())
        {
            throw new IllegalArgumentException("Password must be filled out.");
        }

        if (role == null)
        {
            throw new IllegalArgumentException("Role must be selected.");
        }
    }

    private User createUserObject(int id, String username, String password, UserRole role, LocalDateTime lastLogin)
    {
        if (role == UserRole.UserAdmin)
        {
            return new UserAdmin(id, username.trim(), password, role, lastLogin);
        }
        return new UserScanner(id, username.trim(), password, role, lastLogin);
    }
}
