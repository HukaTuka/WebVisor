package dk.sea.webvisor.BE;

import java.time.LocalDateTime;

public class User
{
    private final int id;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private final UserRole role;
    private LocalDateTime lastLogin;

    public User(int id, String firstName, String lastName, String username, String password, UserRole role, LocalDateTime lastLogin)
    {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.lastLogin = lastLogin;
    }

    // Getters
    public int getId() {return id;}
    public String getFirstName() {return firstName;}
    public String getLastName(){return lastName;}
    public String getUsername(){return username;}
    public String getPassword(){return password;}
    public UserRole getRole(){return role;}
    public String getRoleDisplayName(){return role.getDisplayName();}
    public LocalDateTime getLastLogin(){return lastLogin;};


    // Setters
    public void setFirstName (String fn){this.firstName = fn;}
    public void setLastName (String ln){this.lastName = ln;}
    public void setUsername (String un){this.username = un;}
    public void setPassword(String pw){this.password = pw;}
    public void setLastLogin (LocalDateTime lastLogin){this.lastLogin = lastLogin;}
}
