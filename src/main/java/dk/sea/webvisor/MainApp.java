package dk.sea.webvisor;

// Java Imports
import dk.sea.webvisor.BLL.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;

public class MainApp extends Application
{
    public static void main(String[] args) throws SQLException, IOException
    {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/CSS/styles.css").toExternalForm());
        primaryStage.setTitle("WebVisor Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
