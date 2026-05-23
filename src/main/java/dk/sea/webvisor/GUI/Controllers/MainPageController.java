package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.BE.UserRole;

// Java Imports
import dk.sea.webvisor.DAL.Interface.AuditAware;
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

    private AuditService audit;

    @FXML
    private void initialize()
    {
        showHome();
    }

    public void setUser(User user, AuditService audit)
    {
        this.audit = audit;

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
        openView("/Views/AdminUsersView.fxml");
    }

    @FXML
    private void onOpenHome()
    {
        showHome();
    }

    @FXML
    private void onOpenScanning()
    {
        openView("/Views/ScanningView.fxml");
    }

    @FXML
    private void onOpenAuditLog()
    {
            openView("/Views/AuditLogView.fxml");
    }

    @FXML
    private void onOpenAdminProfiles()
    {
        openView("/Views/AdminProfilesView.fxml");
    }

    @FXML
    private void onOpenAdminClients()
    {
        openView("/Views/AdminClientsView.fxml");
    }

    @FXML
    private void onOpenAdminMetadata()
    {
        openView("/Views/AdminMetadataView.fxml");
    }

    private void openView(String fxmlPath)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            injectAudit(loader.getController());
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

    private void injectAudit(Object controller){
        if (controller instanceof AuditAware a) {
            a.setAudit(audit);
        }
    }
}
