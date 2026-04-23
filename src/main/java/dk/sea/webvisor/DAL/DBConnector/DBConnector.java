package dk.sea.webvisor.DAL.DBConnector;

// SQL Imports
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;

// Java Imports
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

public class DBConnector
{
    private static final String PROP_FILE = "config/config.settings";
    private static SQLServerDataSource dataSource;
    private static DBConnector instance;


    public DBConnector() throws IOException
    {
        Properties databaseProperties = new Properties();
        databaseProperties.load(new FileInputStream(new File(PROP_FILE)));

        dataSource = new SQLServerDataSource();
        dataSource.setServerName(databaseProperties.getProperty("Server"));
        dataSource.setDatabaseName(databaseProperties.getProperty("Database"));
        dataSource.setUser(databaseProperties.getProperty("User"));
        dataSource.setPassword(databaseProperties.getProperty("Password"));
        dataSource.setPortNumber(1433);
        dataSource.setTrustServerCertificate(true);
    }
    public static DBConnector getInstance() throws IOException
    {
        if (instance == null)
        {
            instance = new DBConnector();
        }
        return instance;
    }

    public static Connection getConnection() throws SQLServerException
    {
        return dataSource.getConnection();
    }


    static void main(String[] args) throws Exception
    {
        DBConnector databaseConnector = new DBConnector();

        try (Connection connection = databaseConnector.getConnection())
        {
            System.out.println("Is it open? " + !connection.isClosed());
        }
    }
}
