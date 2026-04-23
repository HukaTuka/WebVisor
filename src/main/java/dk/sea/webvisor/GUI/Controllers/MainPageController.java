package dk.sea.webvisor.GUI.Controllers;

// Java Imports
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainPageController
{
    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize()
    {
        showHome();
    }

    @FXML
    private void onOpenAdminUsers()
    {
        openAdminUsersView();
    }

    @FXML
    private void onOpenHome()
    {
        showHome();
    }

    private void openAdminUsersView()
    {
        try
        {
            Parent adminUsersRoot = FXMLLoader.load(getClass().getResource("/Views/AdminUsersView.fxml"));
            contentArea.getChildren().setAll(adminUsersRoot);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not open Admin Users view.", e);
        }
    }

    private void showHome()
    {
        contentArea.getChildren().clear();
    }
}
