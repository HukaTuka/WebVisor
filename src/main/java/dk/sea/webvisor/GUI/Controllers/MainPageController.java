package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.BE.UserRole;

// Java Imports
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainPageController
{
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox adminMenu;
    @FXML
    private VBox userMenu;

    private final AuditService audit = AuditService.getInstance();

    @FXML
    private void initialize()
    {
        showHome();
    }

    public void setUser(User user)
    {
        // Hide menus first
        setVisible(adminMenu, false);
        setVisible(userMenu, false);

        // Admin gets both menus, scanner gets user menu only
        if (user.getRole() == UserRole.UserAdmin)
        {
            setVisible(adminMenu, true);
            setVisible(userMenu, true);
        }
        else
        {
            setVisible(userMenu, true);
        }

        showHome();
    }

    @FXML
    private void onOpenAdminUsers()
    {
        audit.log("NAVIGATE", "Opened Admin Users view");
        openView("/Views/AdminUsersView.fxml");
    }

    @FXML
    private void onOpenHome()
    {
        audit.log("NAVIGATE", "Navigated to Home");
        showHome();
    }

    @FXML
    private void onOpenScanning()
    {
        audit.log("NAVIGATE", "Opened Scanning view");
        openView("/Views/ScanningView.fxml");
    }

    @FXML
    private void onOpenAuditLog() {
            audit.log("NAVIGATE", "Opened Audit Log window");
            openView("/Views/AuditLogView.fxml");

    }

    @FXML
    private void onOpenAdminProfiles() {
        audit.log("NAVIGATE", "Opened Admin Profiles view");
        openView("/Views/AdminProfilesView.fxml");
    }

    private void openView(String fxmlPath)
    {
        try
        {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(root);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not open view: " + fxmlPath, e);
        }
    }

    private void showHome()
    {
        contentArea.getChildren().clear();
    }

    private void setVisible(VBox node, boolean v)
    {
        node.setVisible(v);
        node.setManaged(v);
    }

    @FXML
    private void onLogout(ActionEvent actionEvent)
    {
        audit.log("LOGOUT", "User logged out");
        audit.clearCurrentUser();

        try
        {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/Views/LoginView.fxml"));
            Scene scene = ((Node) actionEvent.getSource()).getScene();
            scene.setRoot(loginRoot);

            Stage stage = (Stage) scene.getWindow();
            stage.setTitle("WebVisor Login");
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
            throw new IllegalStateException("Could not open login view.", e);
        }
    }
}
