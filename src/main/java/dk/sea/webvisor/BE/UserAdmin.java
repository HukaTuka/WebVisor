package dk.sea.webvisor.BE;

public class UserAdmin extends User
{
    public UserAdmin(int id, String firstName, String lastName, String username, String password, UserRole role)
    {
        super(id, firstName, lastName, username, password, role);
    }
}
