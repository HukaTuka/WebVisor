package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.BLL.UserService;

// Java Imports
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;

public class LoginController
{
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label lblStatus;

    private final UserService userService;
    private final AuditService audit = AuditService.getInstance();

    public LoginController()
    {
        try
        {
            this.userService = new UserService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialize login service.", e);
        }
    }

    @FXML
    private void onLogin()
    {
        String username = txtUsername.getText();

        try
        {
            User user = userService.login(txtUsername.getText(), txtPassword.getText());

            // ── Audit: successful login ───────────────────────────────────────
            audit.setCurrentUser(user.getUsername());
            audit.log("LOGIN", "User logged in successfully. Role: " + user.getRole().getDisplayName());

            openMainPage(user);
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not connect to the user database.", "status-error");
        }
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }

    private void openMainPage(User user)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/MainPageView.fxml"));
            Parent mainPageRoot = loader.load();
            MainPageController mainPageController = loader.getController();
            mainPageController.setUser(user);

            Scene currentScene = txtUsername.getScene();
            if (currentScene == null)
            {
                showStatus("Could not open main page.", "status-error");
                return;
            }

            currentScene.setRoot(mainPageRoot);
            Stage stage = (Stage) currentScene.getWindow();
            stage.setTitle("WebVisor - Main Page");
            stage.setMinWidth(1280);
            stage.setMinHeight(800);
            if (stage.getWidth() < 1280)
            {
                stage.setWidth(1500);
            }
            if (stage.getHeight() < 800)
            {
                stage.setHeight(920);
            }
        }
        catch (IOException e)
        {
            showStatus("Could not open main page.", "status-error");
        }
    }
}
