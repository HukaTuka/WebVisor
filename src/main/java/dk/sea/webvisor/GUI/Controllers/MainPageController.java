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
}
