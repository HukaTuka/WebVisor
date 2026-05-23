package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.Archive;
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.BLL.ArchiveAdminService;
import dk.sea.webvisor.BLL.ClientService;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminClientsController
{
    @FXML private TableView<Client> tblClients;
    @FXML private TableColumn<Client, String> colName;
    @FXML private TableColumn<Client, String> colArchives;
    @FXML private TableColumn<Client, Void> colClientActions;
    @FXML private TableColumn<Client, Void> colArchiveActions;
    @FXML private TextField txtSearch;
    @FXML private Label lblFormHeading;
    @FXML private TextField txtName;
    @FXML private TextField txtArchiveName;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblStatus;

    private final ClientService clientService;
    private final ArchiveAdminService archiveAdminService;
    private AuditService audit;
    private final ObservableList<Client> allClients = FXCollections.observableArrayList();
    private final Map<Integer, List<Archive>> archivesByClient = new HashMap<>();
    private FilteredList<Client> filteredClients;
    private Client editingClient = null;

    public AdminClientsController()
    {
        try
        {
            this.clientService = new ClientService();
            this.archiveAdminService = new ArchiveAdminService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialise Client/Archive services.", e);
        }
    }

    @FXML
    private void initialize()
    {
        setCancelVisible(false);
        setupColumns();
        setupDoubleClick();
        refreshClients();
    }

    private void setupColumns()
    {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colArchives.setCellValueFactory(data ->
                new SimpleStringProperty(getArchiveNamesForClient(data.getValue().getId())));

        colClientActions.setCellFactory(col -> new TableCell<>()
        {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("secondary-button");
                btnDelete.getStyleClass().add("danger-button");

                btnEdit.setOnAction(e ->
                {
                    Client client = getTableView().getItems().get(getIndex());
                    startEditing(client);
                });

                btnDelete.setOnAction(e ->
                {
                    Client client = getTableView().getItems().get(getIndex());
                    handleDeleteClient(client);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty)
            {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        colArchiveActions.setCellFactory(col -> new TableCell<>()
        {
            private final Button btnAddArchive = new Button("Add");
            private final Button btnUpdateArchive = new Button("Rename");
            private final Button btnDeleteArchive = new Button("Delete");
            private final HBox box = new HBox(8, btnAddArchive, btnUpdateArchive, btnDeleteArchive);

            {
                btnAddArchive.getStyleClass().add("secondary-button");
                btnUpdateArchive.getStyleClass().add("secondary-button");
                btnDeleteArchive.getStyleClass().add("danger-button");

                btnAddArchive.setOnAction(e ->
                {
                    Client client = getTableView().getItems().get(getIndex());
                    handleAddArchive(client);
                });

                btnUpdateArchive.setOnAction(e ->
                {
                    Client client = getTableView().getItems().get(getIndex());
                    handleUpdateArchive(client);
                });

                btnDeleteArchive.setOnAction(e ->
                {
                    Client client = getTableView().getItems().get(getIndex());
                    handleDeleteArchive(client);
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
        tblClients.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
            {
                Client selected = tblClients.getSelectionModel().getSelectedItem();
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

    @FXML
    private void onSaveClient()
    {
        if (editingClient == null)
        {
            createClient();
        }
        else
        {
            updateClient();
        }
    }

    @FXML
    private void onCancelEdit()
    {
        clearForm();
    }

    private void applyFilter()
    {
        String query = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredClients.setPredicate(client ->
        {
            if (query.isEmpty()) return true;
            return client.getName().toLowerCase().contains(query)
                    || getArchiveNamesForClient(client.getId()).toLowerCase().contains(query);
        });
    }

    private void createClient()
    {
        Client created = null;
        try
        {
            created = clientService.createClient(txtName.getText());
            String archiveName = txtArchiveName.getText() == null ? "" : txtArchiveName.getText().trim();
            archiveAdminService.createArchive(archiveName, created);


            refreshClients();
            clearForm();
            showStatus("Client \"" + created.getName() + "\" and archive \"" + archiveName + "\" created.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            if (created != null)
            {
                try
                {
                    clientService.deleteClient(created.getId());
                }
                catch (Exception ignored)
                {
                }
            }
            showStatus("Could not save client/archive to database.", "status-error");
        }
    }

    private void updateClient()
    {
        try
        {
            String oldName = editingClient.getName();
            String newName = txtName.getText() == null ? "" : txtName.getText().trim();

            clientService.updateClient(editingClient.getId(), newName);

            refreshClients();
            clearForm();
            showStatus("Client updated.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not update client in database.", "status-error");
        }
    }

    private void handleAddArchive(Client client)
    {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Archive");
        dialog.setHeaderText("Create archive for client: " + client.getName());
        dialog.setContentText("Archive name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty())
        {
            showStatus("Archive creation cancelled.", "status-info");
            return;
        }

        String archiveName = result.get();
        try
        {
            Archive createdArchive = archiveAdminService.createArchive(archiveName, client);

            refreshClients();
            showStatus("Archive \"" + createdArchive.getName() + "\" created.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not create archive in database.", "status-error");
        }
    }

    private void handleDeleteArchive(Client client)
    {
        List<Archive> archives = archivesByClient.getOrDefault(client.getId(), List.of());
        if (archives.isEmpty())
        {
            showStatus("Client has no archives to delete.", "status-error");
            return;
        }

        ChoiceDialog<Archive> dialog = new ChoiceDialog<>(archives.get(0), archives);
        dialog.setTitle("Delete Archive");
        dialog.setHeaderText("Delete archive for client: " + client.getName());
        dialog.setContentText("Choose archive:");

        Optional<Archive> result = dialog.showAndWait();
        if (result.isEmpty())
        {
            showStatus("Archive deletion cancelled.", "status-info");
            return;
        }

        Archive archive = result.get();
        try
        {
            archiveAdminService.deleteArchive(archive.getId());

            refreshClients();
            showStatus("Archive \"" + archive.getName() + "\" deleted.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not delete archive from database.", "status-error");
        }
    }

    private void handleUpdateArchive(Client client)
    {
        List<Archive> archives = archivesByClient.getOrDefault(client.getId(), List.of());
        if (archives.isEmpty())
        {
            showStatus("Client has no archives to update.", "status-error");
            return;
        }

        ChoiceDialog<Archive> pickDialog = new ChoiceDialog<>(archives.get(0), archives);
        pickDialog.setTitle("Rename Archive");
        pickDialog.setHeaderText("Choose archive to rename for client: " + client.getName());
        pickDialog.setContentText("Archive:");

        Optional<Archive> picked = pickDialog.showAndWait();
        if (picked.isEmpty())
        {
            showStatus("Archive update cancelled.", "status-info");
            return;
        }

        Archive selectedArchive = picked.get();

        TextInputDialog renameDialog = new TextInputDialog(selectedArchive.getName());
        renameDialog.setTitle("Rename Archive");
        renameDialog.setHeaderText("Rename archive for client: " + client.getName());
        renameDialog.setContentText("New name:");

        Optional<String> newNameResult = renameDialog.showAndWait();
        if (newNameResult.isEmpty())
        {
            showStatus("Archive rename cancelled.", "status-info");
            return;
        }

        try
        {
            archiveAdminService.updateArchive(selectedArchive.getId(), newNameResult.get(), client);

            refreshClients();
            showStatus("Archive updated.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not update archive in database.", "status-error");
        }
    }

    private void handleDeleteClient(Client client)
    {
        try
        {
            List<String> assignedBoxes = clientService.getBoxIdsAssignedToClient(client.getId());
            if (!assignedBoxes.isEmpty())
            {
                showStatus(
                        "Client cannot be deleted while boxes are assigned: " + String.join(", ", assignedBoxes),
                        "status-error");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete client \"" + client.getName() + "\"?",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(null);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK)
            {
                showStatus("Deletion cancelled.", "status-info");
                return;
            }

            clientService.deleteClient(client.getId());

            if (editingClient != null && editingClient.getId() == client.getId())
            {
                clearForm();
            }

            refreshClients();
            showStatus("Client \"" + client.getName() + "\" deleted.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not delete client from database.", "status-error");
        }
    }

    private void startEditing(Client client)
    {
        editingClient = client;
        txtName.setText(client.getName());
        txtArchiveName.clear();
        txtArchiveName.setDisable(true);

        lblFormHeading.setText("Edit Client: " + client.getName());
        btnSave.setText("Save Changes");
        setCancelVisible(true);

        showStatus("Editing client: " + client.getName(), "status-info");
    }

    private void clearForm()
    {
        editingClient = null;
        txtName.clear();
        txtArchiveName.clear();
        txtArchiveName.setDisable(false);

        lblFormHeading.setText("Create New Client");
        btnSave.setText("Create Client");
        setCancelVisible(false);

        tblClients.getSelectionModel().clearSelection();
        lblStatus.setText("");
    }

    private void refreshClients()
    {
        try
        {
            List<Client> clients = clientService.getAllClients();
            List<Archive> archives = archiveAdminService.getAllArchives();

            archivesByClient.clear();
            for (Archive archive : archives)
            {
                archivesByClient.computeIfAbsent(archive.getClientId(), id -> new ArrayList<>()).add(archive);
            }

            allClients.setAll(clients);

            if (filteredClients == null)
            {
                filteredClients = new FilteredList<>(allClients, c -> true);
                tblClients.setItems(filteredClients);
            }

            applyFilter();
            tblClients.refresh();
        }
        catch (SQLException e)
        {
            showStatus("Could not load clients from database.", "status-error");
        }
    }

    private String getArchiveNamesForClient(int clientId)
    {
        List<Archive> archives = archivesByClient.getOrDefault(clientId, List.of());
        if (archives.isEmpty())
        {
            return "No archives";
        }

        List<String> names = new ArrayList<>();
        for (Archive archive : archives)
        {
            names.add(archive.getName());
        }
        return String.join(", ", names);
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
