package dk.sea.webvisor.BE;

// Java Imports
import java.time.LocalDateTime;

public class UserScanner extends User
{
    public UserScanner(int id, String username, String password, UserRole role, LocalDateTime lastLogin)
    {
        super(id, username, password, role, lastLogin);
    }
}
