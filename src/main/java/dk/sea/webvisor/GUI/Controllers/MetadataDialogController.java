package dk.sea.webvisor.GUI.Controllers;

import dk.sea.webvisor.BE.BoxMetadataEntry;
import dk.sea.webvisor.BE.MetadataField;
import dk.sea.webvisor.BE.MetadataFieldType;
import dk.sea.webvisor.BLL.BoxMetadataService;
import dk.sea.webvisor.BLL.MetadataFieldService;
import dk.sea.webvisor.BLL.Util.AuditService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MetadataDialogController
{
    @FXML private Label lblBoxTitle;
    @FXML private VBox fieldsContainer;
    @FXML private Label lblStatus;
    @FXML private Label lblNoFields;

    private final MetadataFieldService metadataFieldService;
    private final BoxMetadataService boxMetadataService;
    private final AuditService audit = AuditService.getInstance();

    private String boxId;
    private List<BoxMetadataEntry> entries;
    private final List<TextField> inputFields = new ArrayList<>();

    public MetadataDialogController()
    {
        try
        {
            this.metadataFieldService = new MetadataFieldService();
            this.boxMetadataService = new BoxMetadataService();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not initialise metadata services.", e);
        }
    }

    public void setBox(String boxId)
    {
        this.boxId = boxId;
        lblBoxTitle.setText("Box Metadata — " + boxId);

        try
        {
            List<MetadataField> fields = metadataFieldService.getAllFields();

            if (fields.isEmpty())
            {
                lblNoFields.setVisible(true);
                lblNoFields.setManaged(true);
                return;
            }

            lblNoFields.setVisible(false);
            lblNoFields.setManaged(false);

            entries = boxMetadataService.getMetadataForBox(boxId, fields);
            buildForm();
        }
        catch (SQLException e)
        {
            showStatus("Could not load metadata: " + e.getMessage(), "status-error");
        }
    }

    private void buildForm()
    {
        fieldsContainer.getChildren().clear();
        inputFields.clear();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(160);
        labelCol.setPrefWidth(160);

        ColumnConstraints inputCol = new ColumnConstraints();
        inputCol.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(labelCol, inputCol);

        for (int i = 0; i < entries.size(); i++)
        {
            BoxMetadataEntry entry = entries.get(i);

            Label label = new Label(entry.getField().getName() + ":");

            TextField input = new TextField(entry.getValue());
            input.setMaxWidth(Double.MAX_VALUE);

            if (entry.getField().getFieldType() == MetadataFieldType.NUMBER)
            {
                input.setPromptText("Enter a number");
            }
            else if (entry.getField().getFieldType() == MetadataFieldType.DATE)
            {
                input.setPromptText("YYYY-MM-DD");
            }
            else
            {
                input.setPromptText("Enter text");
            }

            grid.add(label, 0, i);
            grid.add(input, 1, i);
            inputFields.add(input);
        }

        fieldsContainer.getChildren().add(grid);
    }

    @FXML
    private void onSave()
    {
        if (entries == null || entries.isEmpty())
        {
            return;
        }

        try
        {
            for (int i = 0; i < entries.size(); i++)
            {
                BoxMetadataEntry entry = entries.get(i);
                String value = inputFields.get(i).getText();

                boxMetadataService.saveMetadata(
                        boxId,
                        entry.getField().getId(),
                        entry.getField().getFieldType(),
                        value
                );
            }

            audit.log("METADATA_SAVED", "Saved metadata for box: " + boxId);
            showStatus("Metadata saved successfully.", "status-success");
        }
        catch (IllegalArgumentException e)
        {
            showStatus(e.getMessage(), "status-error");
        }
        catch (SQLException e)
        {
            showStatus("Could not save metadata to database.", "status-error");
        }
    }

    @FXML
    private void onClose()
    {
        Stage stage = (Stage) lblBoxTitle.getScene().getWindow();
        stage.close();
    }

    private void showStatus(String message, String styleClass)
    {
        lblStatus.getStyleClass().removeAll("status-success", "status-error", "status-info");
        lblStatus.getStyleClass().add(styleClass);
        lblStatus.setText(message);
    }
}