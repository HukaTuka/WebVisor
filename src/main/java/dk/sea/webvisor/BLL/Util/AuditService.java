package dk.sea.webvisor.BLL.Util;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;
import dk.sea.webvisor.DAL.DAO.AuditDAO;

// Java Imports
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.sql.SQLException;

//Audit service for creating logs through out the system using dependency injection
public class AuditService
{
    private final ObservableList<AuditEntry> entries = FXCollections.observableArrayList();
    private AuditDAO auditDAO;
    private String   currentUser = "anonymous";

    public AuditService()
    {
        try {
            auditDAO = new AuditDAO();
        } catch (IOException e) {
            System.err.println("AuditService Could not initialise AuditDAO: " + e.getMessage());
        }
    }

    //Sets the user on log in, so that logs are made in their username
    public void setCurrentUser(String username)
    {
        this.currentUser = (username != null && !username.isBlank()) ? username : "anonymous";
    }

    //Sets the user back to anonymous on log out
    public void clearCurrentUser()
    {
        this.currentUser = "anonymous";
    }

    //Logs the action and writes to the database
    public void log(String action, String details)
    {
        AuditEntry entry = new AuditEntry(currentUser, action, details);
        entries.add(entry);

        if (auditDAO != null)
        {
            try
            {
                auditDAO.insertAuditEntry(entry);
            }
            catch (SQLException e)
            {
                System.err.println("[AuditService] Failed to write audit entry to DB: ");
            }
        }

    }

    //Loads the database into an observable list
    public void loadFromDatabase()
    {
        if (auditDAO == null) return;

        try
        {
            entries.setAll(auditDAO.getAllAuditEntries());
        }
        catch (SQLException e)
        {
            System.err.println("[AuditService] Failed to load audit entries from DB: ");
        }
    }

    public String getCurrentUser()
    {
        return currentUser;
    }

    //Returns the observable list to be bound to a table view
    public ObservableList<AuditEntry> getEntries()
    {
        return entries;
    }
}