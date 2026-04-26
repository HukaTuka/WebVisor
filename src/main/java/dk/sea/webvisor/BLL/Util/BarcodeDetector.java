package dk.sea.webvisor.BLL.Util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utility that inspects a {@link BufferedImage} for any barcode or QR-code
 * using the ZXing ("Zebra Crossing") library.
 *
 * A positive result means the API has signalled a <em>document split</em>:
 * the current document should be closed and a new one started.
 *
 */
public class BarcodeDetector
{
    private BarcodeDetector() {}

    /**
     * Returns {@code true} when a recognisable barcode is found in the image.
     * This method never throws – any decoding failure is treated as "no barcode".
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
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            new MultiFormatReader().decode(bitmap, hints);
            return true; // decode succeeded → barcode found
        }
        catch (NotFoundException e)
        {
            return false; // no barcode in this image
        }
    }
}
