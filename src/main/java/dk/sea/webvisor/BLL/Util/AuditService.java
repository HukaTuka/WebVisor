package dk.sea.webvisor.BLL.Util;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;
import dk.sea.webvisor.DAL.DAO.AuditDAO;

// Java Imports
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Application-wide audit trail service (singleton).
 *
 * Writes every log entry to the database via AuditDAO, and keeps
 * an in-memory ObservableList so any bound TableView updates live.
 *
 * Usage:
 *   AuditService.getInstance().log("CREATE_USER", "Created user: john.doe");
 */
public class AuditService
{
    // Singleton

    private static AuditService instance;

    private AuditService()
    {
        try
        {
            auditDAO = new AuditDAO();
        }
        catch (IOException e)
        {
            System.err.println("[AuditService] Could not initialise AuditDAO: " + e.getMessage());
        }
    }

    public static AuditService getInstance()
    {
        if (instance == null)
        {
            instance = new AuditService();
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final ObservableList<AuditEntry> entries = FXCollections.observableArrayList();
    private AuditDAO auditDAO;
    private String   currentUser = "anonymous";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sets the currently logged-in user. Call immediately after a successful
     * login so all subsequent entries are attributed correctly.
     */
    public void setCurrentUser(String username)
    {
        this.currentUser = (username != null && !username.isBlank()) ? username : "anonymous";
    }

    /**
     * Resets the current user back to "anonymous". Call on logout.
     */
    public void clearCurrentUser()
    {
        this.currentUser = "anonymous";
    }

    /**
     * Logs an action performed by the current user.
     * Writes to the database and updates the in-memory list.
     *
     * @param action  Short action identifier, e.g. "CREATE_USER".
     * @param details Human-readable description of what happened.
     */
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
                System.err.println("[AuditService] Failed to write audit entry to DB: " + e.getMessage());
            }
        }

    }

    /**
     * Loads the full audit history from the database into the observable list.
     * Call this when opening the audit log view so it shows all historical entries,
     * not just those from the current session.
     */
    public void loadFromDatabase()
    {
        if (auditDAO == null) return;

        try
        {
            entries.setAll(auditDAO.getAllAuditEntries());
        }
        catch (SQLException e)
        {
            System.err.println("[AuditService] Failed to load audit entries from DB: " + e.getMessage());
        }
    }

    /**
     * Returns the live observable list — bind a TableView to this directly.
     */
    public ObservableList<AuditEntry> getEntries()
    {
        return entries;
    }
}