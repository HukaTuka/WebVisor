package dk.sea.webvisor.GUI.Managers;

import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.BE.Profile;
import dk.sea.webvisor.BE.User;
import dk.sea.webvisor.BE.UserRole;
import dk.sea.webvisor.BLL.ProfileService;
import dk.sea.webvisor.BLL.ProfileUserService;
import dk.sea.webvisor.BLL.UserService;
import dk.sea.webvisor.BLL.Util.AuditService;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProfileDropdownManager
{
    private final ProfileService profileService;
    private final ProfileUserService profileUserService;
    private final UserService userService;
    private final AuditService auditService;
    private final ComboBox<Profile> cmbProfile;
    private final ComboBox<Integer> CmbSessionRotation;
    private final UiManager uiManager;

    public  ProfileDropdownManager(ProfileService profileService,
                                   ProfileUserService profileUserService,
                                   UserService userService,
                                   AuditService audit,
                                   ComboBox<Profile> cmbProfile,
                                   ComboBox<Integer> CmbSessionRotation,
                                   UiManager uiManager) {
        this.profileService = profileService;
        this.profileUserService = profileUserService;
        this.userService = userService;
        this.auditService = audit;
        this.cmbProfile = cmbProfile;
        this.CmbSessionRotation = CmbSessionRotation;
        this.uiManager = uiManager;
    }


    public void setup(ComboBox<Client> cmbClient, ComboBox<Integer> cmbSessionRotation) {
        cmbClient.valueProperty().addListener((obs, old, val) ->
                refreshProfilesForClient(val));

        cmbProfile.valueProperty().addListener((obs, old, val) -> {
            if (val != null){
                cmbSessionRotation.setValue(val.getDefaultRotation());
            }
        });

        refreshProfilesForClient(cmbClient.getValue());
    }


    public void refreshProfilesForClient(Client client) {
        try
        {
            List<Profile> profiles = new ArrayList<>();

            if (client == null || client.getId() < 0) {
                profiles = new ArrayList<>(profileService.getProfilesByClient(client.getId()));
            }
            else {
                profiles = new ArrayList<>(profileService.getProfilesByClient(client.getId()));
            }

            String currentUsername = auditService.getCurrentUser();
            if (currentUsername != null && !currentUsername.isBlank()) {
                Optional<User> currentUser = userService.getUserByUsername(currentUsername);
                if (currentUser.isPresent() && currentUser.get().getRole() != UserRole.UserAdmin) {
                    List<Profile> assignedToUser = profileUserService.getProfilesForUser(currentUser.get().getId());
                    profiles.retainAll(assignedToUser);
                }
            }

            cmbProfile.setItems(FXCollections.observableArrayList(profiles));
            cmbProfile.setValue(null);
        }
        catch (SQLException e) {
            uiManager.error("Could not load profiles");
        }
    }
}
