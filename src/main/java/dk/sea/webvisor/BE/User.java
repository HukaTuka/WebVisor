package dk.sea.webvisor.BE;

public class User
{
    private final int id;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private final UserRole role;

    public User(int id, String firstName, String lastName, String username, String password, UserRole role)
    {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters
    public int getId() {return id;}
    public String getFirstName() {return firstName;}
    public String getLastName(){return lastName;}
    public String getUsername(){return username;}
    public String getPassword(){return password;}
    public UserRole getRole(){return role;}
    public String getRoleDisplayName(){return role.getDisplayName();}

    // Setters
    public void setFirstName (String fn){this.firstName = fn;}
    public void setLastName (String ln){this.lastName = ln;}
    public void setUsername (String un){this.username = un;}
    public void setPassword(String pw){this.password = pw;}
}
