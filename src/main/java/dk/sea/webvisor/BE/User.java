package dk.sea.webvisor.BE;

//Project Imports
import dk.sea.webvisor.BE.Profile;

// Java Imports
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class User
{
    private final int id;
    private String username;
    private String password;
    private final UserRole role;
    private LocalDateTime lastLogin;
    private List<Profile> assignedProfiles = new ArrayList<>();

    public User(int id,  String username, String password, UserRole role, LocalDateTime lastLogin)
    {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.lastLogin = lastLogin;
    }

    // Getters
    public int getId() {return id;}
    public String getUsername(){return username;}
    public String getPassword(){return password;}
    public UserRole getRole(){return role;}
    public LocalDateTime getLastLogin(){return lastLogin;}
    public List<Profile> getAssignedProfiles() {return assignedProfiles;}


    // Setters
    public void setUsername (String un){this.username = un;}
    public void setPassword(String pw){this.password = pw;}
    public void setLastLogin (LocalDateTime lastLogin){this.lastLogin = lastLogin;}
    public void setAssignedProfiles(List<Profile> assignedProfiles){this.assignedProfiles = assignedProfiles == null ? new ArrayList<>() : assignedProfiles;}
}
