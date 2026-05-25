package dk.sea.webvisor.BE;

public enum UserRole
{
    UserScanner("Scanner"),
    UserAdmin("Administrator"),
    UserQA("QA Reviewer");

    private final String displayName;

    UserRole(String displayName) {this.displayName = displayName;}
    public String getDisplayName() {return displayName;}
}
