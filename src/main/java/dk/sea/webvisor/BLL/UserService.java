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

        return user;
    }

    public List<User> getAllUsers() throws SQLException
    {
        return usersDAO.getAllUsers();
    }

    public User createUser(String firstName, String lastName, String username, String plainPassword, UserRole role) throws SQLException
    {
        validateUserInput(firstName, lastName, username, plainPassword, role);

        String hashedPassword = PasswordHasher.hashPassword(plainPassword.trim());
        User user = createUserObject(0, firstName, lastName, username, hashedPassword, role);
        return usersDAO.createUser(user);
    }

    public void updateUser(int userId, String firstName, String lastName, String username, String plainPassword, UserRole role) throws SQLException
    {
        if (userId <= 0)
        {
            throw new IllegalArgumentException("Please choose a valid user.");
        }

        validateUserInput(firstName, lastName, username, plainPassword, role);

        String hashedPassword = PasswordHasher.hashPassword(plainPassword.trim());
        User user = createUserObject(userId, firstName, lastName, username, hashedPassword, role);
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

    private void validateUserInput(String firstName, String lastName, String username, String plainPassword, UserRole role)
    {
        if (firstName == null || firstName.isBlank())
        {
            throw new IllegalArgumentException("First name must be filled out.");
        }

        if (lastName == null || lastName.isBlank())
        {
            throw new IllegalArgumentException("Last name must be filled out.");
        }

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

    private User createUserObject(int id, String firstName, String lastName, String username, String password, UserRole role)
    {
        if (role == UserRole.UserAdmin)
        {
            return new UserAdmin(id, firstName.trim(), lastName.trim(), username.trim(), password, role);
        }
        return new UserScanner(id, firstName.trim(), lastName.trim(), username.trim(), password, role);
    }
}
