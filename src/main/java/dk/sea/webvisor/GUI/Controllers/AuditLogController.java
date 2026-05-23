package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;
import dk.sea.webvisor.BLL.Util.AuditService;

// Java Imports
import dk.sea.webvisor.DAL.Interface.AuditAware;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.Collectors;
import java.time.LocalDate;

public class AuditLogController implements AuditAware
{
    @FXML private TableView<AuditEntry> tblAuditLog;
    @FXML private TableColumn<AuditEntry, String> colTimestamp;
    @FXML private TableColumn<AuditEntry, String> colUsername;
    @FXML private TableColumn<AuditEntry, String> colAction;
    @FXML private TableColumn<AuditEntry, String> colDetails;

    @FXML private TextField txtFilterUsername;
    @FXML private ComboBox<String> cmbFilterAction;
    @FXML private DatePicker dpFilterFrom;
    @FXML private DatePicker dpFilterTo;
    @FXML private Label lblEntryCount;

    private AuditService audit;

    private FilteredList<AuditEntry> filteredEntries;

    @Override
    public void setAudit(AuditService audit) {
        this.audit = audit;

        // Load full history from DB, then wrap in a FilteredList for live filtering
        this.audit.loadFromDatabase();
        filteredEntries = new FilteredList<>(this.audit.getEntries(), e -> true);
        tblAuditLog.setItems(filteredEntries);

        populateActionCombo();
        filterListener();
        updateEntryCount();
    }

    @FXML
    private void initialize()
    {
        setupColumns();
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
        dpFilterFrom.setValue(null);
        dpFilterTo.setValue(null);
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

    //"listens" to the search filter and then filters without the need for an apply button
    private void filterListener()
    {
        txtFilterUsername.textProperty().addListener((obs, o, n) -> applyFilters());
        cmbFilterAction.valueProperty().addListener((obs, o, n) -> applyFilters());

        if (dpFilterFrom != null) {
            dpFilterFrom.valueProperty().addListener((obs, o, n) -> applyFilters());
        }

        if (dpFilterTo != null)
        {
            dpFilterTo.valueProperty().addListener((obs, o, n) -> applyFilters());
        }
    }

    private void populateActionCombo()
    {
        // Build the list of unique action names from whatever is already loaded
        ObservableList<String> actions = FXCollections.observableArrayList(
                audit.getEntries().stream()
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
        String actionFilter = cmbFilterAction.getSelectionModel().getSelectedItem();
        LocalDate from = dpFilterFrom.getValue();
        LocalDate to = dpFilterTo.getValue();

        filteredEntries.setPredicate(entry ->
        {
            boolean matchesUsername = usernameFilter.isEmpty()
                    || entry.getUsername().toLowerCase().contains(usernameFilter);

            boolean matchesAction = actionFilter == null || actionFilter.isBlank()
                    || entry.getAction().equalsIgnoreCase(actionFilter);

            boolean afterFrom = true;
            boolean beforeTo = true;

            if (entry.getTimestamp() != null)
            {
                LocalDate entryDate = entry.getTimestamp().toLocalDate();
                afterFrom = (from == null) || !entryDate.isBefore(from);
                beforeTo = (to == null) || !entryDate.isAfter(to);
            }

            return matchesUsername && matchesAction && afterFrom && beforeTo;
        });

        updateEntryCount();
    }

    private void updateEntryCount()
    {
        int shown = filteredEntries.size();
        int total = audit.getEntries().size();

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