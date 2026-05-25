package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.AuditEntry;
import dk.sea.webvisor.BLL.Util.AuditService;

// Apache Imports
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Java Imports
import dk.sea.webvisor.DAL.Interface.AuditAware;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

    @FXML
    private void onExportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Audit Log");
        fileChooser.setInitialFileName("AuditLog.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

        Stage stage = (Stage) tblAuditLog.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Log");

            //Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            //Header row
            Row header = sheet.createRow(0);
            String[] columns = {"Timestamp", "Username", "Action", "Details"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            //Data from the filtered entries gets put in the rows
            int rowNum = 1;
            for (AuditEntry entry : filteredEntries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getFormattedTimestamp());
                row.createCell(1).setCellValue(entry.getUsername());
                row.createCell(2).setCellValue(entry.getAction());
                row.createCell(3).setCellValue(entry.getDetails());
            }

            //Make the columns the right size
            for(int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(file)){
                workbook.write(out);
            }

            //On a success we get feedback
            lblEntryCount.setText("exported " + filteredEntries.size() + " entries.");

        } catch (IOException e) {
            lblEntryCount.setText("Export failed: " + e.getMessage());
        }
    }
}