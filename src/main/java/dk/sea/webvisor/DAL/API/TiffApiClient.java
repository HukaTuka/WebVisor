package dk.sea.webvisor.DAL.API;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.*;


    /**
     * DAL-layer HTTP client for the WebLager TIFF scanning API.
     *
     * The API always returns a ZIP archive, even when a single file is requested.
     * Each ZIP entry is decoded with {@link ImageIO} – Java 9+ includes a built-in
     * TIFF plugin so no extra dependency is required for image reading.
     */
    public class TiffApiClient
    {
        private static final String BASE_URL   = "https://studentiffapi-production.up.railway.app";
        private static final int    CONNECT_MS = 10_000;
        private static final int    READ_MS    = 20_000;

        /**
         * Calls {@code GET /getRandomFile} and returns all images found inside
         * the ZIP response.  The list will normally contain one image, but may
         * occasionally contain a barcode page instead of (or alongside) a
         * document page.
         *
         * @return a non-null, possibly-empty list of {@link BufferedImage} objects
         * @throws IOException if the HTTP call fails or the ZIP cannot be parsed
         */
        public List<BufferedImage> fetchRandomPage() throws IOException
        {
            HttpURLConnection conn = openConnection("/getRandomFile");

            try (InputStream body = conn.getInputStream())
            {
                return unzipImages(body);
            }
            finally
            {
                conn.disconnect();
            }
        }

        private HttpURLConnection openConnection(String path) throws IOException
        {
            URL url = new URL(BASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_MS);
            conn.setReadTimeout(READ_MS);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK)
            {
                conn.disconnect();
                throw new IOException("TIFF API returned HTTP " + status + " for " + path);
            }

            return conn;
        }

        /**
         * Reads every entry in the ZIP stream.  Each entry's bytes are buffered
         * fully before being handed to {@link ImageIO} so that closing one entry
         * does not interfere with reading others.
         */
        private List<BufferedImage> unzipImages(InputStream source) throws IOException
        {
            List<BufferedImage> images = new ArrayList<>();

            try (ZipInputStream zip = new ZipInputStream(source))
            {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null)
                {
                    if (!entry.isDirectory())
                    {
                        byte[] bytes = zip.readAllBytes();
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                        if (img != null)
                        {
                            images.add(img);
                        }
                    }
                    zip.closeEntry();
                }
            }

            return images;
        }
    }
