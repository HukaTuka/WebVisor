package dk.sea.webvisor.BE;

// Java Imports
import java.time.LocalDateTime;

public class UserAdmin extends User
{
    public UserAdmin(int id, String username, String password, UserRole role, LocalDateTime lastLogin)
    {
        super(id, username, password, role, lastLogin);
    }
}
