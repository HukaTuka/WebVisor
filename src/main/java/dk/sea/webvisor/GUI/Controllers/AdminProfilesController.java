package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.BLL.ProfileService;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the Admin Profiles view.
 *
 * Follows the same GUI-layer conventions used by {@link AdminUsersController}:
 * all business logic is delegated to {@link ProfileService}, and every
 * meaningful action is recorded through {@link AuditService}.
 */
public class AdminProfilesController
{

    @FXML private TableView<Profile> tblProfiles;
    @FXML private TableColumn<Profile, String> colName;
    @FXML private TableColumn<Profile, Boolean> colSplitOnBarcode;
    @FXML private TableColumn<Profile, Integer> colDefaultRotation;
    @FXML private TextField txtName;
    @FXML private CheckBox chkSplitOnBarcode;
    @FXML private TextField txtRotation;
    @FXML private Label lblStatus;
    private final ProfileService profileService;
    private final AuditService   audit = AuditService.getInstance();
    private Profile selectedProfile = null;

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
        setupColumns();
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
    }

    @FXML
    private void onTableClicked()
    {
        Profile clicked = tblProfiles.getSelectionModel().getSelectedItem();
        if (clicked == null)
        {
            return;
        }

        selectedProfile = clicked;
        txtName.setText(clicked.getName());
        chkSplitOnBarcode.setSelected(clicked.isSplitOnBarcode());
        txtRotation.setText(String.valueOf(clicked.getDefaultRotation()));

        showStatus("Profile selected: " + clicked.getName(), "status-info");
    }

    @FXML
    private void onCreateProfile()
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

    @FXML
    private void onUpdateProfile()
    {
        if (selectedProfile == null)
        {
            showStatus("Select a profile from the table first.", "status-error");
            return;
        }

        try
        {
            String  oldName     = selectedProfile.getName();
            int     oldRotation = selectedProfile.getDefaultRotation();
            boolean oldSplit    = selectedProfile.isSplitOnBarcode();

            int newRotation = parseRotation();

            profileService.updateProfile(
                    selectedProfile.getId(),
                    txtName.getText(),
                    chkSplitOnBarcode.isSelected(),
                    newRotation
            );

            audit.log("UPDATE_PROFILE",
                    "Updated profile ID " + selectedProfile.getId()
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

    @FXML
    private void onDeleteProfile()
    {
        if (selectedProfile == null)
        {
            showStatus("Select a profile from the table first.", "status-error");
            return;
        }

        try
        {
            // Check if any users are assigned to this profile
            List<String> assignedUsers =
                    profileService.getUsernamesAssignedToProfile(selectedProfile.getId());

            if (!assignedUsers.isEmpty())
            {
                // Show a warning and ask for confirmation before deleting
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
                        "Deletion of profile \"" + selectedProfile.getName()
                                + "\" (ID: " + selectedProfile.getId()
                                + ") was confirmed despite being assigned to: " + userList);
            }
            else
            {
                // No users assigned still asks for basic confirmation
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete profile \"" + selectedProfile.getName() + "\"?",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText(null);
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK)
                {
                    showStatus("Deletion cancelled.", "status-info");
                    return;
                }
            }

            String deletedName = selectedProfile.getName();
            int    deletedId   = selectedProfile.getId();

            profileService.deleteProfile(deletedId);

            audit.log("DELETE_PROFILE",
                    "Deleted profile: \"" + deletedName + "\" (ID: " + deletedId + ")");

            refreshProfiles();
            clearForm();
            showStatus("Profile \"" + deletedName + "\" deleted.", "status-success");
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

    /**
     * Parses the rotation text field.  Accepts any integer (positive, negative,
     * or > 360) and returns the raw value.  Normalisation to a 0-359 range is handled by the service layer.
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

    private void refreshProfiles()
    {
        try
        {
            List<Profile> profiles = profileService.getAllProfiles();
            tblProfiles.setItems(FXCollections.observableArrayList(profiles));
        }
        catch (SQLException e)
        {
            showStatus("Could not load profiles from database.", "status-error");
        }
    }

    private void clearForm()
    {
        selectedProfile = null;
        tblProfiles.getSelectionModel().clearSelection();
        txtName.clear();
        chkSplitOnBarcode.setSelected(true);
        txtRotation.setText("0");
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }
}