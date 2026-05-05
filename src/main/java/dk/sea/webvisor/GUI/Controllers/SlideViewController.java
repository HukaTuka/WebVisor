package dk.sea.webvisor.GUI.Controllers;

// Project Imports
import dk.sea.webvisor.BE.Files;

// Java Imports
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.embed.swing.SwingFXUtils;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.List;

public class SlideViewController
{
    @FXML private BorderPane rootPane;
    @FXML private ImageView imgSlide;
    @FXML private Label     lblPageInfo;
    @FXML private Label     lblBarcodeWarning;

    private List<Files> pages;
    private int currentIndex = 0;

    public void setPages(List<Files> pages, int startIndex)
    {
        this.pages = pages;
        this.currentIndex = startIndex;
        showPage(currentIndex);

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) ->
        {
            if (newScene != null)
            {
                newScene.setOnKeyPressed(event ->
                {
                    if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.DOWN)
                    {
                        next();
                    }
                    else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.UP)
                    {
                        prev();
                    }
                    else if (event.getCode() == KeyCode.ESCAPE)
                    {
                        close();
                    }
                });

                // Request focus on root so arrow keys work immediately
                rootPane.requestFocus();
            }
        });
    }

    @FXML
    private void onPrev() {
        prev();
    }

    @FXML
    private void onNext() {
        next();
    }

    @FXML
    private void onClose() {
        close();
    }

    private void prev()
    {
        if (currentIndex > 0)
        {
            showPage(--currentIndex);
        }
    }

    private void next()
    {
        if (currentIndex < pages.size() - 1)
        {
            showPage(++currentIndex);
        }
    }

    private void close()
    {
        rootPane.getScene().getWindow().hide();
    }

    private void showPage(int index)
    {
        Files page = pages.get(index);

        BufferedImage raw = page.getImage();
        if (raw != null)
        {
            BufferedImage rotated = applyAwtRotation(raw, page.getRotationDegrees());
            imgSlide.setImage(SwingFXUtils.toFXImage(rotated, null));
        }
        else
        {
            imgSlide.setImage(null);
        }

        imgSlide.setOpacity(page.isBarcode() ? 0.55 : 1.0);
        lblPageInfo.setText((index + 1) + " / " + pages.size() + "  —  " + page.getReferenceId());
        lblBarcodeWarning.setVisible(page.isBarcode());
        lblBarcodeWarning.setManaged(page.isBarcode());
    }

    private BufferedImage applyAwtRotation(BufferedImage src, int degrees)
    {
        if (degrees == 0) return src;

        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newW = (int) Math.floor(src.getWidth() * cos + src.getHeight() * sin);
        int newH = (int) Math.floor(src.getWidth() * sin + src.getHeight() * cos);

        BufferedImage rotated = new BufferedImage(newW, newH,
                src.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : src.getType());

        AffineTransform tx = new AffineTransform();
        tx.translate((newW - src.getWidth()) / 2.0, (newH - src.getHeight()) / 2.0);
        tx.rotate(radians, src.getWidth() / 2.0, src.getHeight() / 2.0);

        new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR).filter(src, rotated);
        return rotated;
    }
}