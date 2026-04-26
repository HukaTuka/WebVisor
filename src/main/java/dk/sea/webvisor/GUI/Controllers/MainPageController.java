package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.UserRole;

// Java Imports
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainPageController
{
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox adminMenu;
    @FXML
    private VBox userMenu;

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
}
