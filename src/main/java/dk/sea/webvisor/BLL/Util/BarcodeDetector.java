package dk.sea.webvisor.BLL.Util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Utility that inspects a {@link BufferedImage} for a barcode or QR-code
 * using the ZXing ("Zebra Crossing") library.
 *
 * A positive result means the API has signalled a document split:
 * the current document should be closed and a new one started.
 *
 * Only the barcode formats that the WebLager scanning API uses as
 * document-split signals are checked. Restricting the format set
 * prevents ZXing from misidentifying incidental visual patterns
 * (such as redaction marks or dense text lines) as barcodes.
 */
public class BarcodeDetector
{
    private BarcodeDetector() {}

    /**
     * The barcode formats recognised as a document-split signal.
     * TRY_HARDER is intentionally omitted: it makes ZXing very
     * aggressive and is the primary cause of false positives on
     * scanned documents with linear markings.
     */
    private static final List<BarcodeFormat> ACCEPTED_FORMATS = Arrays.asList(
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX
    );

    /**
     * Returns {@code true} when a recognisable barcode is found in the image.
     * This method never throws; any decoding failure is treated as "no barcode".
     */
    public static boolean isBarcode(BufferedImage image)
    {
        if (image == null)
        {
            return false;
        }

        try
        {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, ACCEPTED_FORMATS);

            new MultiFormatReader().decode(bitmap, hints);
            return true;
        }
        catch (NotFoundException e)
        {
            return false;
        }
    }
}