package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.stream.Collectors;

public class AuditLogController
{
    @FXML private TableView<AuditEntry>        tblAuditLog;
    @FXML private TableColumn<AuditEntry, String> colTimestamp;
    @FXML private TableColumn<AuditEntry, String> colUsername;
    @FXML private TableColumn<AuditEntry, String> colAction;
    @FXML private TableColumn<AuditEntry, String> colDetails;

    @FXML private TextField         txtFilterUsername;
    @FXML private ComboBox<String>  cmbFilterAction;
    @FXML private Label             lblEntryCount;

    private final AuditService auditService = AuditService.getInstance();

    private FilteredList<AuditEntry> filteredEntries;

    @FXML
    private void initialize()
    {
        setupColumns();

        // Load full history from DB, then wrap in a FilteredList for live filtering
        auditService.loadFromDatabase();
        filteredEntries = new FilteredList<>(auditService.getEntries(), e -> true);
        tblAuditLog.setItems(filteredEntries);

        populateActionCombo();
        updateEntryCount();
    }

    @FXML
    private void onFilterChanged()
    {
        applyFilters();
    }

    @FXML
    private void onClearFilters()
    {
        txtFilterUsername.clear();
        cmbFilterAction.getSelectionModel().clearSelection();
        applyFilters();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void setupColumns()
    {
        colTimestamp.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFormattedTimestamp()));

        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));

        colAction.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAction()));

        colDetails.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDetails()));
    }

    private void populateActionCombo()
    {
        // Build the list of unique action names from whatever is already loaded
        ObservableList<String> actions = FXCollections.observableArrayList(
                auditService.getEntries().stream()
                        .map(AuditEntry::getAction)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList())
        );
        cmbFilterAction.setItems(actions);
    }

    private void applyFilters()
    {
        String usernameFilter = txtFilterUsername.getText().trim().toLowerCase();
        String actionFilter   = cmbFilterAction.getSelectionModel().getSelectedItem();

        filteredEntries.setPredicate(entry ->
        {
            boolean matchesUsername = usernameFilter.isEmpty()
                    || entry.getUsername().toLowerCase().contains(usernameFilter);

            boolean matchesAction = actionFilter == null || actionFilter.isBlank()
                    || entry.getAction().equalsIgnoreCase(actionFilter);

            return matchesUsername && matchesAction;
        });

        updateEntryCount();
    }

    private void updateEntryCount()
    {
        int shown = filteredEntries.size();
        int total = auditService.getEntries().size();

        if (shown == total)
        {
            lblEntryCount.setText(total + " entries");
        }
        else
        {
            lblEntryCount.setText(shown + " of " + total + " entries");
        }
    }
}