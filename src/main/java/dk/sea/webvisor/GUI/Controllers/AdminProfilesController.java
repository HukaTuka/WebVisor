package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.BLL.ProfileService;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminProfilesController
{
    @FXML private TableView<Profile>            tblProfiles;
    @FXML private TableColumn<Profile, String>  colName;
    @FXML private TableColumn<Profile, Boolean> colSplitOnBarcode;
    @FXML private TableColumn<Profile, Integer> colDefaultRotation;
    @FXML private TableColumn<Profile, Void>    colActions;
    @FXML private TextField txtSearch;
    @FXML private Label    lblFormHeading;
    @FXML private TextField txtName;
    @FXML private CheckBox  chkSplitOnBarcode;
    @FXML private TextField txtRotation;
    @FXML private Button    btnSave;
    @FXML private Button    btnCancel;
    @FXML private Label lblStatus;
    private final ProfileService profileService;
    private final AuditService   audit = AuditService.getInstance();

    private final ObservableList<Profile> allProfiles = FXCollections.observableArrayList();
    private FilteredList<Profile>         filteredProfiles;

    /** Non-null while an edit is in progress; null means "create" mode. */
    private Profile editingProfile = null;

    public AdminProfilesController()
    {
        try
        {
            this.profileService = new ProfileService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialise ProfileService.", e);
        }
    }


    @FXML
    private void initialize()
    {
        setCancelVisible(false);

        setupColumns();
        setupDoubleClick();
        refreshProfiles();
    }

    private void setupColumns()
    {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colSplitOnBarcode.setCellValueFactory(new PropertyValueFactory<>("splitOnBarcode"));
        colSplitOnBarcode.setCellFactory(col -> new TableCell<>()
        {
            @Override
            protected void updateItem(Boolean value, boolean empty)
            {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : (value ? "Yes" : "No"));
            }
        });

        colDefaultRotation.setCellValueFactory(new PropertyValueFactory<>("defaultRotation"));
        colDefaultRotation.setCellFactory(col -> new TableCell<>()
        {
            @Override
            protected void updateItem(Integer value, boolean empty)
            {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value + "°");
            }
        });

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
                    Profile profile = getTableView().getItems().get(getIndex());
                    startEditing(profile);
                });

                btnDelete.setOnAction(e ->
                {
                    Profile profile = getTableView().getItems().get(getIndex());
                    handleDelete(profile);
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
        tblProfiles.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY
                    && event.getClickCount() == 2)
            {
                Profile selected = tblProfiles.getSelectionModel().getSelectedItem();
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

        filteredProfiles.setPredicate(profile ->
        {
            if (query.isEmpty()) return true;
            return profile.getName().toLowerCase().contains(query);
        });
    }

    @FXML
    private void onSaveProfile()
    {
        if (editingProfile == null)
        {
            createProfile();
        }
        else
        {
            updateProfile();
        }
    }

    @FXML
    private void onCancelEdit()
    {
        clearForm();
    }

    private void createProfile()
    {
        try
        {
            Profile created = profileService.createProfile(
                    txtName.getText(),
                    chkSplitOnBarcode.isSelected(),
                    parseRotation()
            );

            audit.log("CREATE_PROFILE",
                    "Created profile: \"" + created.getName()
                            + "\" | rotation: " + created.getDefaultRotation()
                            + "° | split on barcode: " + created.isSplitOnBarcode());

            refreshProfiles();
            clearForm();
            showStatus("Profile \"" + created.getName() + "\" created.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not save profile to database.", "status-error");
        }
    }

    private void updateProfile()
    {
        try
        {
            String  oldName     = editingProfile.getName();
            int     oldRotation = editingProfile.getDefaultRotation();
            boolean oldSplit    = editingProfile.isSplitOnBarcode();
            int     newRotation = parseRotation();

            profileService.updateProfile(
                    editingProfile.getId(),
                    txtName.getText(),
                    chkSplitOnBarcode.isSelected(),
                    newRotation
            );

            audit.log("UPDATE_PROFILE",
                    "Updated profile ID " + editingProfile.getId()
                            + " | name: \"" + oldName + "\" -> \"" + txtName.getText().trim() + "\""
                            + " | rotation: " + oldRotation + "° -> " + Profile.normaliseRotation(newRotation) + "°"
                            + " | split on barcode: " + oldSplit + " -> " + chkSplitOnBarcode.isSelected());

            refreshProfiles();
            clearForm();
            showStatus("Profile updated.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not update profile in database.", "status-error");
        }
    }

    private void handleDelete(Profile profile)
    {
        try
        {
            List<String> assignedUsers =
                    profileService.getUsernamesAssignedToProfile(profile.getId());

            if (!assignedUsers.isEmpty())
            {
                String userList = String.join(", ", assignedUsers);
                Alert warning = new Alert(Alert.AlertType.CONFIRMATION);
                warning.setTitle("Profile in Use");
                warning.setHeaderText("This profile is assigned to " + assignedUsers.size()
                        + " user(s): " + userList);
                warning.setContentText(
                        "Deleting it will remove the assignment for all listed users. "
                                + "Do you want to continue?");

                Optional<ButtonType> result = warning.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK)
                {
                    showStatus("Deletion cancelled.", "status-info");
                    return;
                }

                audit.log("DELETE_PROFILE_WARNED",
                        "Deletion of profile \"" + profile.getName()
                                + "\" (ID: " + profile.getId()
                                + ") confirmed despite being assigned to: " + userList);
            }
            else
            {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete profile \"" + profile.getName() + "\"?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText(null);

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK)
                {
                    showStatus("Deletion cancelled.", "status-info");
                    return;
                }
            }

            profileService.deleteProfile(profile.getId());

            audit.log("DELETE_PROFILE",
                    "Deleted profile: \"" + profile.getName()
                            + "\" (ID: " + profile.getId() + ")");

            if (editingProfile != null && editingProfile.getId() == profile.getId())
            {
                clearForm();
            }

            refreshProfiles();
            showStatus("Profile \"" + profile.getName() + "\" deleted.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not delete profile from database.", "status-error");
        }
    }

    private void startEditing(Profile profile)
    {
        editingProfile = profile;
        txtName.setText(profile.getName());
        chkSplitOnBarcode.setSelected(profile.isSplitOnBarcode());
        txtRotation.setText(String.valueOf(profile.getDefaultRotation()));

        lblFormHeading.setText("Edit Profile: " + profile.getName());
        btnSave.setText("Save Changes");
        setCancelVisible(true);

        showStatus("Editing profile: " + profile.getName(), "status-info");
    }

    private void clearForm()
    {
        editingProfile = null;
        txtName.clear();
        chkSplitOnBarcode.setSelected(true);
        txtRotation.setText("0");

        lblFormHeading.setText("Create New Profile");
        btnSave.setText("Create Profile");
        setCancelVisible(false);

        tblProfiles.getSelectionModel().clearSelection();
        lblStatus.setText("");
    }

    private void refreshProfiles()
    {
        try
        {
            List<Profile> profiles = profileService.getAllProfiles();
            allProfiles.setAll(profiles);

            if (filteredProfiles == null)
            {
                filteredProfiles = new FilteredList<>(allProfiles, p -> true);
                tblProfiles.setItems(filteredProfiles);
            }

            applyFilter();
        }
        catch (SQLException e)
        {
            showStatus("Could not load profiles from database.", "status-error");
        }
    }

    /**
     * Parses the rotation text field. Accepts any integer normalisation to
     * [0, 359] is handled by the service layer.
     *
     * @throws IllegalArgumentException if the field is not a valid integer.
     */
    private int parseRotation()
    {
        String raw = txtRotation.getText() == null ? "" : txtRotation.getText().trim();

        if (raw.isEmpty())
        {
            return 0;
        }

        try
        {
            return Integer.parseInt(raw);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(
                    "Rotation must be a whole number (e.g. 0, 90, 180, 270).");
        }
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