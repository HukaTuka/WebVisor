package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.UserRole;
import dk.sea.webvisor.BLL.UserService;

// Java Imports
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AdminUsersController
{
    @FXML
    private TableView<User> tblUsers;
    @FXML
    private TableColumn<User, String> colFirstName;
    @FXML
    private TableColumn<User, String> colLastName;
    @FXML
    private TableColumn<User, String> colUsername;
    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private TextField txtFirstName;
    @FXML
    private TextField txtLastName;
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<String> cmbRole;
    @FXML
    private Label lblStatus;

    private final UserService userService;
    private User selectedUser;

    public AdminUsersController()
    {
        try
        {
            this.userService = new UserService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialize user service.", e);
        }
    }

    @FXML
    private void initialize()
    {
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleDisplayName"));

        cmbRole.setItems(FXCollections.observableArrayList("Administrator", "Scanner"));
        cmbRole.getSelectionModel().select("Scanner");

        refreshUsers();
    }

    @FXML
    private void onTableClicked()
    {
        selectedUser = tblUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null)
        {
            return;
        }

        txtFirstName.setText(selectedUser.getFirstName());
        txtLastName.setText(selectedUser.getLastName());
        txtUsername.setText(selectedUser.getUsername());
        txtPassword.clear();
        cmbRole.getSelectionModel().select(selectedUser.getRole().getDisplayName());
        showStatus("User selected. Enter new password to update.", "status-info");
    }

    @FXML
    private void onCreateUser()
    {
        try
        {
            User createdUser = userService.createUser(
                    txtFirstName.getText(),
                    txtLastName.getText(),
                    txtUsername.getText(),
                    txtPassword.getText(),
                    getSelectedRole()
            );

            refreshUsers();
            clearForm();
            showStatus("Created user: " + createdUser.getUsername(), "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not create user in database.", "status-error");
        }
    }

    @FXML
    private void onUpdateUser()
    {
        if (selectedUser == null)
        {
            showStatus("Choose a user from the table first.", "status-error");
            return;
        }

        try
        {
            userService.updateUser
                    (
                    selectedUser.getId(),
                    txtFirstName.getText(),
                    txtLastName.getText(),
                    txtUsername.getText(),
                    txtPassword.getText(),
                    getSelectedRole()
                    );

            refreshUsers();
            clearForm();
            showStatus("User updated.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not update user in database.", "status-error");
        }
    }

    @FXML
    private void onDeleteUser()
    {
        if (selectedUser == null)
        {
            showStatus("Choose a user from the table first.", "status-error");
            return;
        }

        try
        {
            userService.deleteUser(selectedUser.getId());
            refreshUsers();
            clearForm();
            showStatus("User deleted.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not delete user from database.", "status-error");
        }
    }

    private UserRole getSelectedRole()
    {
        String roleText = cmbRole.getSelectionModel().getSelectedItem();
        if ("Administrator".equalsIgnoreCase(roleText))
        {
            return UserRole.UserAdmin;
        }
        return UserRole.UserScanner;
    }

    private void refreshUsers()
    {
        try
        {
            List<User> users = userService.getAllUsers();
            tblUsers.setItems(FXCollections.observableArrayList(users));
        }
        catch (SQLException e)
        {
            showStatus("Could not load users from database.", "status-error");
        }
    }

    private void clearForm()
    {
        selectedUser = null;
        tblUsers.getSelectionModel().clearSelection();
        txtFirstName.clear();
        txtLastName.clear();
        txtUsername.clear();
        txtPassword.clear();
        cmbRole.getSelectionModel().select("Scanner");
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }
}
