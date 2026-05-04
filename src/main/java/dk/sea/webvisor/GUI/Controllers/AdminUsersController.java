package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.UserRole;
import dk.sea.webvisor.BLL.Util.AuditService;
import dk.sea.webvisor.BLL.UserService;

// Java Imports
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AdminUsersController
{
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private TextField txtSearch;
    @FXML private Label lblFormHeading;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblStatus;
    private final UserService  userService;
    private final AuditService audit = AuditService.getInstance();

    private final ObservableList<User> allUsers = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;

    /** Non-null while an edit is in progress; null means "create" mode. */
    private User editingUser = null;

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
        // Hide cancel here, after @FXML injection, so JavaFX layout is aware
        // of the initial state before any layout pass runs.
        setCancelVisible(false);

        cmbRole.setItems(FXCollections.observableArrayList("Administrator", "Scanner"));
        cmbRole.getSelectionModel().select("Scanner");

        setupColumns();
        setupDoubleClick();
        refreshUsers();
    }

    private void setupColumns()
    {
        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));

        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole().getDisplayName()));

        // Inline Edit / Delete buttons
        colActions.setCellFactory(col -> new TableCell<>()
        {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("danger-button");

                btnEdit.setOnAction(e ->
                {
                    User user = getTableView().getItems().get(getIndex());
                    startEditing(user);
                });

                btnDelete.setOnAction(e ->
                {
                    User user = getTableView().getItems().get(getIndex());
                    handleDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty)
            {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupDoubleClick()
    {
        tblUsers.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY
                    && event.getClickCount() == 2)
            {
                User selected = tblUsers.getSelectionModel().getSelectedItem();
                if (selected != null)
                {
                    startEditing(selected);
                }
            }
        });
    }

    @FXML
    private void onSearchChanged()
    {
        applyFilter();
    }

    @FXML
    private void onClearSearch()
    {
        txtSearch.clear();
        applyFilter();
    }

    private void applyFilter()
    {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredUsers.setPredicate(user ->
        {
            if (query.isEmpty()) return true;
            return user.getUsername().toLowerCase().contains(query)
                    || user.getRole().getDisplayName().toLowerCase().contains(query);
        });
    }

    @FXML
    private void onSaveUser()
    {
        if (editingUser == null)
        {
            createUser();
        }
        else
        {
            updateUser();
        }
    }

    @FXML
    private void onCancelEdit()
    {
        clearForm();
    }

    private void createUser()
    {
        try
        {
            User created = userService.createUser(
                    txtUsername.getText(),
                    txtPassword.getText(),
                    getSelectedRole(),
                    LocalDateTime.now()
            );

            audit.log("CREATE_USER", "Created new user: " + created.getUsername()
                    + " | Role: " + created.getRole().getDisplayName());

            refreshUsers();
            clearForm();
            showStatus("Created user: " + created.getUsername(), "status-success");
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

    private void updateUser()
    {
        try
        {
            String oldUsername = editingUser.getUsername();
            String oldRole     = editingUser.getRole().getDisplayName();
            String newUsername = txtUsername.getText();
            String newRole     = getSelectedRole().getDisplayName();

            userService.updateUser(
                    editingUser.getId(),
                    newUsername,
                    txtPassword.getText(),
                    getSelectedRole(),
                    LocalDateTime.now()
            );

            audit.log("UPDATE_USER", "Updated user ID " + editingUser.getId()
                    + " | Username: " + oldUsername + " -> " + newUsername
                    + " | Role: " + oldRole + " -> " + newRole
                    + (txtPassword.getText().isBlank() ? "" : " | Password changed"));

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

    private void handleDelete(User user)
    {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user \"" + user.getUsername() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK)
        {
            showStatus("Deletion cancelled.", "status-info");
            return;
        }

        try
        {
            userService.deleteUser(user.getId());

            audit.log("DELETE_USER", "Deleted user: " + user.getUsername()
                    + " (ID: " + user.getId() + ")");

            if (editingUser != null && editingUser.getId() == user.getId())
            {
                clearForm();
            }

            refreshUsers();
            showStatus("User \"" + user.getUsername() + "\" deleted.", "status-success");
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

    private void startEditing(User user)
    {
        editingUser = user;
        txtUsername.setText(user.getUsername());
        txtPassword.clear();
        cmbRole.getSelectionModel().select(user.getRole().getDisplayName());

        lblFormHeading.setText("Edit User: " + user.getUsername());
        btnSave.setText("Save Changes");
        setCancelVisible(true);

        showStatus("Editing user: " + user.getUsername()
                + ". Enter a new password to change it.", "status-info");
    }

    private void clearForm()
    {
        editingUser = null;
        txtUsername.clear();
        txtPassword.clear();
        cmbRole.getSelectionModel().select("Scanner");

        lblFormHeading.setText("Create New User");
        btnSave.setText("Create User");
        setCancelVisible(false);

        tblUsers.getSelectionModel().clearSelection();
        lblStatus.setText("");
    }

    private void refreshUsers()
    {
        try
        {
            List<User> users = userService.getAllUsers();
            allUsers.setAll(users);

            if (filteredUsers == null)
            {
                filteredUsers = new FilteredList<>(allUsers, u -> true);
                tblUsers.setItems(filteredUsers);
            }

            applyFilter();
        }
        catch (SQLException e)
        {
            showStatus("Could not load users from database.", "status-error");
        }
    }

    private UserRole getSelectedRole()
    {
        String roleText = cmbRole.getSelectionModel().getSelectedItem();
        return "Administrator".equalsIgnoreCase(roleText)
                ? UserRole.UserAdmin
                : UserRole.UserScanner;
    }

    private void setCancelVisible(boolean visible)
    {
        btnCancel.setVisible(visible);
        btnCancel.setManaged(visible);
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }
}