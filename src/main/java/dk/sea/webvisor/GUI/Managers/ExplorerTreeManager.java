package dk.sea.webvisor.GUI.Managers;

// Project Imports
import dk.sea.webvisor.BE.Archive;
import dk.sea.webvisor.BE.Boxes;
import dk.sea.webvisor.BE.Client;
import dk.sea.webvisor.BE.Document;
import dk.sea.webvisor.BE.Files;
import dk.sea.webvisor.BLL.ArchiveService;
import dk.sea.webvisor.GUI.Controllers.ScanningController;

// Java Imports
import javafx.collections.FXCollections;
import javafx.scene.control.TreeCell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExplorerTreeManager {

    private final TreeView<Object> treeView;
    @SuppressWarnings("unused")
    private final UiManager uiManager;

    public ExplorerTreeManager(TreeView<Object> treeView, UiManager uiManager, Consumer<Object> onSelect) {
        this.treeView = treeView;
        this.uiManager = uiManager;

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                onSelect.accept(newVal.getValue());
            }
        });
    }

    public List<Boxes> filterBoxes(List<Boxes> allBoxes, Client client, Archive archive) {
        List<Boxes> filtered = new ArrayList<>();
        if (client == null || client.getId() <= 0) {
            return filtered;
        }
        if (archive == null) {
            return filtered;
        }

        for (Boxes box : allBoxes) {
            boolean matchesClient = box.getClient().equalsIgnoreCase(client.getName());
            boolean matchesArchive = box.getArchiveId() == archive.getId();
            if (matchesClient && matchesArchive) {
                filtered.add(box);
            }
        }
        return filtered;
    }

    public void showFilteredBoxes(List<Boxes> allBoxes, Client client, Archive archive) {
        List<Boxes> filtered = filterBoxes(allBoxes, client, archive);
        showBoxes(filtered);
    }

    public ScanningController.Level getLevelForSelected(Object selected) {
        if (selected instanceof Boxes) return ScanningController.Level.BOXES;
        if (selected instanceof Document) return ScanningController.Level.DOCUMENTS;
        return ScanningController.Level.FILES;
    }

    public Document resolveSelectedDocument(Boxes selectedBox, Document selectedDocument) {
        if (selectedBox == null || selectedDocument == null) {
            return null;
        }

        for (Document doc : selectedBox.getDocuments()) {
            if (doc == selectedDocument) {
                return doc;
            }
            if (doc.getId() > 0 && selectedDocument.getId() > 0 && doc.getId() == selectedDocument.getId()) {
                return doc;
            }
            if (doc.getDocumentNumber() == selectedDocument.getDocumentNumber()) {
                return doc;
            }
        }
        return null;
    }

    public void refreshArchivesForClient(ComboBox<Archive> cmbArchive, List<Archive> allArchives, Client client) {
        if (cmbArchive == null) {
            return;
        }

        if (client == null || client.getId() <= 0) {
            cmbArchive.setItems(FXCollections.observableArrayList());
            cmbArchive.getSelectionModel().clearSelection();
            cmbArchive.setDisable(true);
            return;
        }

        List<Archive> filtered = new ArrayList<>();
        for (Archive archive : allArchives) {
            if (archive.getClientId() == client.getId()) {
                filtered.add(archive);
            }
        }

        cmbArchive.setItems(FXCollections.observableArrayList(filtered));
        cmbArchive.setDisable(filtered.isEmpty());
        cmbArchive.getSelectionModel().clearSelection();
    }

    public int findPageIndex(List<Files> pages, Files selectedPage) {
        if (selectedPage.getId() > 0) {
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).getId() == selectedPage.getId()) {
                    return i;
                }
            }
        }
        return pages.indexOf(selectedPage);
    }

    public Object getSelectedValue() {
        TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getValue();
    }

    public void showBoxes(List<Boxes> boxes) {
        TreeItem<Object> root = new TreeItem<>();
        root.setExpanded(true);
        for (Boxes box : boxes) {
            root.getChildren().add(new TreeItem<>(box));
        }
        treeView.setRoot(root);
        treeView.setShowRoot(false);
    }

    public void expandBoxPreservingState(Boxes box)
    {
        TreeItem<Object> boxNode = findBoxNode(box.getBoxId());
        if (boxNode == null)
        {
            expandBox(box);
            return;
        }

        // Remember which document numbers were expanded before rebuild
        java.util.Set<Integer> expandedDocNumbers = new java.util.HashSet<>();
        for (TreeItem<Object> child : boxNode.getChildren())
        {
            if (child.isExpanded() && child.getValue() instanceof Document doc)
            {
                expandedDocNumbers.add(doc.getDocumentNumber());
            }
        }

        // Rebuild the box  as normal
        boxNode.getChildren().clear();
        for (Document document : box.getDocuments())
        {
            TreeItem<Object> docNode = new TreeItem<>(document);

            // Re-expand and re-populate documents that were open before
            if (expandedDocNumbers.contains(document.getDocumentNumber()))
            {
                for (Files file : document.getPages())
                {
                    docNode.getChildren().add(new TreeItem<>(file));
                }
                docNode.setExpanded(true);
            }

            boxNode.getChildren().add(docNode);
        }

        boxNode.setExpanded(true);
    }
    public void expandBox(Boxes box)
    {
        TreeItem<Object> node = findBoxNode(box.getBoxId());
        if (node == null)
        {
            return;
        }

        node.getChildren().clear();
        for (Document document : box.getDocuments())
        {
            node.getChildren().add(new TreeItem<>(document));
        }
        node.setExpanded(true);
        treeView.getSelectionModel().select(node);
    }

    public void expandDocument(Boxes selectedBox, Document document) {
        if (selectedBox == null || document == null) {
            return;
        }

        TreeItem<Object> documentNode = findDocumentNode(selectedBox.getBoxId(), document);
        if (documentNode == null) {
            return;
        }

        documentNode.getChildren().clear();
        for (Files file : document.getPages()) {
            documentNode.getChildren().add(new TreeItem<>(file));
        }
        documentNode.setExpanded(true);
        treeView.getSelectionModel().select(documentNode);
    }

    public Boxes getOwnerBoxForSelected() {
        TreeItem<Object> selected = treeView.getSelectionModel().getSelectedItem();
        return getOwnerBox(selected);
    }

    public void setupTreeInteractions(
            Supplier<ScanningController.Level> levelSupplier,
            BooleanSupplier runningSupplier,
            BiConsumer<Files, Files> onReorder,
            Consumer<Files> onSingleFileClick,
            Runnable onDoubleClick,
            BiConsumer<Files, Document> onMoveToDocument)
    {
        AtomicReference<Files> draggedPage = new AtomicReference<>();

        treeView.setCellFactory(tree -> new TreeCell<>()
        {
            {
                setOnDragDetected(ev -> {
                    if (getItem() instanceof Files file && !runningSupplier.getAsBoolean())
                    {
                        draggedPage.set(file);
                        Dragboard db = startDragAndDrop(TransferMode.MOVE);
                        ClipboardContent cc = new ClipboardContent();
                        cc.putString("reorder");
                        db.setContent(cc);
                        ev.consume();
                    }
                });

                setOnDragOver(ev -> {
                    if (draggedPage.get() != null)
                    {
                        if (getItem() instanceof Files target && draggedPage.get() != target)
                        {
                            ev.acceptTransferModes(TransferMode.MOVE);
                            setStyle("-fx-background-color: derive(-fx-control-inner-background, -10%);");
                        }
                        else if (getItem() instanceof Document)
                        {
                            ev.acceptTransferModes(TransferMode.MOVE);
                            setStyle("-fx-background-color: derive(-fx-control-inner-background, -10%);");
                        }
                        else
                        {
                            setStyle("");
                        }
                    }
                });

                setOnDragExited(ev -> setStyle(""));

                setOnDragDropped(ev -> {
                    setStyle("");
                    if (draggedPage.get() != null)
                    {
                        if (getItem() instanceof Files target)
                        {
                            onReorder.accept(draggedPage.get(), target);
                        }
                        else if (getItem() instanceof Document targetDoc)
                        {
                            onMoveToDocument.accept(draggedPage.get(), targetDoc);
                        }
                    }
                    ev.setDropCompleted(true);
                });

                setOnDragDone(ev -> {
                    setStyle("");
                    draggedPage.set(null);
                });
            }

            @Override
            protected void updateItem(Object item, boolean empty)
            {
                super.updateItem(item, empty);
                getStyleClass().removeAll("box-item-cell", "document-item-cell", "file-item-cell");
                if (empty || item == null) {setText(null); return;}
                if (item instanceof Boxes box) {
                    setText(box.getBoxId() + " (" + box.getDocumentCount() + " docs, " + box.getFileCount() + " files)");
                    getStyleClass().add("box-item-cell");}
                else if (item instanceof Document doc) {
                    setText(doc.toString());
                    getStyleClass().add("document-item-cell");}
                else if (item instanceof Files file) {
                    setText(file.isBarcode() ? file.getReferenceId() + " [BARCODE]" : file.getReferenceId());
                    getStyleClass().add("file-item-cell");}
            }
        });

        treeView.setOnMouseClicked(ev -> {
            if (ev.getButton() != MouseButton.PRIMARY) return;
            Object selected = getSelectedValue();
            if (ev.getClickCount() == 1 && selected instanceof Files file)
            {
                onSingleFileClick.accept(file);
                return;
            }
            if (ev.getClickCount() == 2) onDoubleClick.run();
        });
    }

    public Boxes createBox(String id,
                           Client client,
                           Archive archive,
                           ArchiveService archiveService,
                           List<Boxes> allBoxes) throws Exception {
        String trimmedId = id == null ? "" : id.trim();
        if (trimmedId.isBlank()) {
            throw new IllegalArgumentException("Enter Box ID.");
        }
        if (client == null || client.getId() <= 0) {
            throw new IllegalArgumentException("Select client.");
        }
        if (archive == null) {
            throw new IllegalArgumentException("Select archive.");
        }

        Boxes newBox = archiveService.createBox(trimmedId, client.getName(), archive.getName());
        allBoxes.add(newBox);
        return newBox;
    }

    private Boxes getOwnerBox(TreeItem<Object> node) {
        TreeItem<Object> current = node;
        while (current != null) {
            if (current.getValue() instanceof Boxes box) {
                return box;
            }
            current = current.getParent();
        }
        return null;
    }

    private TreeItem<Object> findBoxNode(String boxId) {
        if (treeView.getRoot() == null) {
            return null;
        }
        for (TreeItem<Object> item : treeView.getRoot().getChildren()) {
            if (item.getValue() instanceof Boxes box && box.getBoxId().equalsIgnoreCase(boxId)) {
                return item;
            }
        }
        return null;
    }

    private TreeItem<Object> findDocumentNode(String boxId, Document targetDocument) {
        if (treeView.getRoot() == null) {
            return null;
        }
        TreeItem<Object> boxNode = findBoxNode(boxId);
        if (boxNode == null) {
            return null;
        }
        for (TreeItem<Object> docNode : boxNode.getChildren()) {
            if (docNode.getValue() instanceof Document doc) {
                if (doc == targetDocument) {
                    return docNode;
                }
                if (doc.getId() > 0 && targetDocument.getId() > 0 && doc.getId() == targetDocument.getId()) {
                    return docNode;
                }
                if (doc.getDocumentNumber() == targetDocument.getDocumentNumber()) {
                    return docNode;
                }
            }
        }
        return null;
    }
}

