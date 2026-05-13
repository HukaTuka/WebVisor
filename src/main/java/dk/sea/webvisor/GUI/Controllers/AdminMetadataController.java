package dk.sea.webvisor.GUI.Controllers;

import dk.sea.webvisor.BE.MetadataField;
import dk.sea.webvisor.BE.MetadataFieldType;
import dk.sea.webvisor.BLL.MetadataFieldService;
import dk.sea.webvisor.BLL.Util.AuditService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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

public class AdminMetadataController
{
    @FXML private TableView<MetadataField>            tblFields;
    @FXML private TableColumn<MetadataField, String>  colName;
    @FXML private TableColumn<MetadataField, String>  colFieldType;
    @FXML private TableColumn<MetadataField, Void>    colActions;
    @FXML private TextField   txtSearch;
    @FXML private Label       lblFormHeading;
    @FXML private TextField   txtName;
    @FXML private ComboBox<MetadataFieldType> cmbFieldType;
    @FXML private Button      btnSave;
    @FXML private Button      btnCancel;
    @FXML private Label       lblStatus;

    private final MetadataFieldService metadataFieldService;
    private final AuditService audit = AuditService.getInstance();

    private final ObservableList<MetadataField> allFields = FXCollections.observableArrayList();
    private FilteredList<MetadataField> filteredFields;

    private MetadataField editingField = null;

    public AdminMetadataController()
    {
        try
        {
            this.metadataFieldService = new MetadataFieldService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialise MetadataFieldService.", e);
        }
    }

    @FXML
    private void initialize()
    {
        setCancelVisible(false);
        cmbFieldType.setItems(FXCollections.observableArrayList(MetadataFieldType.values()));
        cmbFieldType.getSelectionModel().select(MetadataFieldType.TEXT);
        setupColumns();
        setupDoubleClick();
        refreshFields();
    }

    private void setupColumns()
    {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colFieldType.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getFieldType().getDisplayName()
                )
        );

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
                    MetadataField field = getTableView().getItems().get(getIndex());
                    startEditing(field);
                });

                btnDelete.setOnAction(e ->
                {
                    MetadataField field = getTableView().getItems().get(getIndex());
                    handleDelete(field);
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
        tblFields.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
            {
                MetadataField selected = tblFields.getSelectionModel().getSelectedItem();
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
    private void onSaveField()
    {
        if (editingField == null)
        {
            createField();
        }
        else
        {
            updateField();
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
        filteredFields.setPredicate(field ->
        {
            if (query.isEmpty()) return true;
            return field.getName().toLowerCase().contains(query)
                    || field.getFieldType().getDisplayName().toLowerCase().contains(query);
        });
    }

    private void createField()
    {
        try
        {
            MetadataFieldType selectedType = cmbFieldType.getSelectionModel().getSelectedItem();
            if (selectedType == null)
            {
                showStatus("Select a field type.", "status-error");
                return;
            }

            MetadataField created = metadataFieldService.createField(txtName.getText(), selectedType);

            audit.log("CREATE_METADATA_FIELD",
                    "Created metadata field: \"" + created.getName()
                            + "\" | type: " + created.getFieldType().getDisplayName());

            refreshFields();
            clearForm();
            showStatus("Field \"" + created.getName() + "\" created.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not save field to database.", "status-error");
        }
    }

    private void updateField()
    {
        try
        {
            MetadataFieldType selectedType = cmbFieldType.getSelectionModel().getSelectedItem();
            if (selectedType == null)
            {
                showStatus("Select a field type.", "status-error");
                return;
            }

            String oldName = editingField.getName();
            metadataFieldService.updateField(editingField.getId(), txtName.getText(), selectedType);

            audit.log("UPDATE_METADATA_FIELD",
                    "Updated metadata field ID " + editingField.getId()
                            + " | name: \"" + oldName + "\" -> \"" + txtName.getText().trim() + "\""
                            + " | type: " + selectedType.getDisplayName());

            refreshFields();
            clearForm();
            showStatus("Field updated.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not update field in database.", "status-error");
        }
    }

    private void handleDelete(MetadataField field)
    {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete field \"" + field.getName() + "\"?\n\n"
                        + "All metadata values stored under this field will also be deleted.",
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
            metadataFieldService.deleteField(field.getId());

            audit.log("DELETE_METADATA_FIELD",
                    "Deleted metadata field: \"" + field.getName()
                            + "\" (ID: " + field.getId() + ")");

            if (editingField != null && editingField.getId() == field.getId())
            {
                clearForm();
            }

            refreshFields();
            showStatus("Field \"" + field.getName() + "\" deleted.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not delete field from database.", "status-error");
        }
    }

    private void startEditing(MetadataField field)
    {
        editingField = field;
        txtName.setText(field.getName());
        cmbFieldType.getSelectionModel().select(field.getFieldType());

        lblFormHeading.setText("Edit Field: " + field.getName());
        btnSave.setText("Save Changes");
        setCancelVisible(true);

        showStatus("Editing field: " + field.getName(), "status-info");
    }

    private void clearForm()
    {
        editingField = null;
        txtName.clear();
        cmbFieldType.getSelectionModel().select(MetadataFieldType.TEXT);

        lblFormHeading.setText("Create New Field");
        btnSave.setText("Create Field");
        setCancelVisible(false);

        tblFields.getSelectionModel().clearSelection();
        lblStatus.setText("");
    }

    private void refreshFields()
    {
        try
        {
            List<MetadataField> fields = metadataFieldService.getAllFields();
            allFields.setAll(fields);

            if (filteredFields == null)
            {
                filteredFields = new FilteredList<>(allFields, f -> true);
                tblFields.setItems(filteredFields);
            }

            applyFilter();
        }
        catch (SQLException e)
        {
            showStatus("Could not load fields from database.", "status-error");
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