package dk.sea.webvisor.GUI.Controllers;

import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.BLL.ClientService;
import dk.sea.webvisor.BLL.Util.AuditService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

public class AdminClientsController
{
    @FXML private TableView<Client> tblClients;
    @FXML private TableColumn<Client, String> colName;
    @FXML private TableColumn<Client, Void> colClientActions;
    @FXML private TextField txtSearch;
    @FXML private Label lblFormHeading;
    @FXML private TextField txtName;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblStatus;

    private final ClientService clientService;
    private final AuditService audit = AuditService.getInstance();
    private final ObservableList<Client> allClients = FXCollections.observableArrayList();
    private FilteredList<Client> filteredClients;
    private Client editingClient = null;

    public AdminClientsController()
    {
        try
        {
            this.clientService = new ClientService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialise Client services.", e);
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
                query.isEmpty() || client.getName().toLowerCase().contains(query));
    }

    private void createClient()
    {
        Client created = null;
        try
        {
            created = clientService.createClient(txtName.getText());
            audit.log("CREATE_CLIENT", "Created client: \"" + created.getName() + "\"");
            refreshClients();
            clearForm();
            showStatus("Client \"" + created.getName() + "\" created.", "status-success");
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
            showStatus("Could not save client to database.", "status-error");
        }
    }

    private void updateClient()
    {
        try
        {
            String oldName = editingClient.getName();
            String newName = txtName.getText() == null ? "" : txtName.getText().trim();

            clientService.updateClient(editingClient.getId(), newName);
            audit.log("UPDATE_CLIENT", "Updated client ID " + editingClient.getId()
                    + " | name: \"" + oldName + "\" -> \"" + newName + "\"");
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
            audit.log("DELETE_CLIENT", "Deleted client: \"" + client.getName() + "\" (ID: " + client.getId() + ")");

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

        lblFormHeading.setText("Edit Client: " + client.getName());
        btnSave.setText("Save Changes");
        setCancelVisible(true);

        showStatus("Editing client: " + client.getName(), "status-info");
    }

    private void clearForm()
    {
        editingClient = null;
        txtName.clear();

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
            allClients.setAll(clientService.getAllClients());

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
