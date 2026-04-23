package dk.sea.webvisor.BE;

public enum UserRole
{
    UserScanner("Scanner"),
    UserAdmin("Administrator");

    private final String displayName;

    UserRole(String displayName) {this.displayName = displayName;}
    public String getDisplayName() {return displayName;}
}
