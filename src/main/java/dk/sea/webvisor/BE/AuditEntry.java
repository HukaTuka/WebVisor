package dk.sea.webvisor.BE;

// Java Imports
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single audit log entry recording a user action with a timestamp.
 */
public class AuditEntry
{
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime timestamp;
    private final String        username;
    private final String        action;
    private final String        details;

    public AuditEntry(String username, String action, String details)
    {
        this.timestamp = LocalDateTime.now();
        this.username  = username;
        this.action    = action;
        this.details   = details;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public LocalDateTime getTimestamp() { return timestamp; }
    public String        getUsername()  { return username;  }
    public String        getAction()    { return action;    }
    public String        getDetails()   { return details;   }

    public void overrideTimestamp(LocalDateTime dbTimestamp)
    {
        this.timestamp = dbTimestamp;
    }

    /** Returns the timestamp as a formatted string (yyyy-MM-dd HH:mm:ss). */
    public String getFormattedTimestamp()
    {
        return timestamp.format(FORMATTER);
    }

    /** Returns a single log-file line for this entry. */
    public String toLogLine()
    {
        return String.format("[%s] USER: %-20s | ACTION: %-25s | %s",
                getFormattedTimestamp(), username, action, details);
    }

    @Override
    public String toString()
    {
        return toLogLine();
    }
}