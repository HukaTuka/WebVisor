package dk.sea.webvisor.BE;

import java.time.LocalDateTime;

public class UserAdmin extends User
{
    public UserAdmin(int id, String firstName, String lastName, String username, String password, UserRole role, LocalDateTime lastLogin)
    {
        super(id, firstName, lastName, username, password, role, lastLogin);
    }
}
