package net.mine_diver.optiforge.util;

import com.google.common.io.Files;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class Util {

    /**
     * Download from the given {@link URL} to the given {@link File} so long as there are differences between them.
     *
     * @param from The URL of the file to be downloaded
     * @param to The destination to be saved to, and compared against if it exists
     * @param logger The logger to print everything to
     * @throws IOException If an exception occurs during the process
     */
    public static boolean downloadIfChanged(URL from, File to, Logger logger) throws IOException {
        return downloadIfChanged(from, to, logger, false);
    }

    /**
     * Download from the given {@link URL} to the given {@link File} so long as there are differences between them.
     *
     * @param from The URL of the file to be downloaded
     * @param to The destination to be saved to, and compared against if it exists
     * @param logger The logger to print information to
     * @param quiet Whether to only print warnings (when <code>true</code>) or everything
     * @throws IOException If an exception occurs during the process
     */
    public static boolean downloadIfChanged(URL from, File to, Logger logger, boolean quiet) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) from.openConnection();

        //If the output already exists we'll use it's last modified time
        if (to.exists()) {
            connection.setIfModifiedSince(to.lastModified());
        }

        //Try use the ETag if there's one for the file we're downloading
        String etag = loadETag(to, logger);

        if (etag != null) {
            connection.setRequestProperty("If-None-Match", etag);
        }

        //We want to download gzip compressed stuff
        connection.setRequestProperty("Accept-Encoding", "gzip");

        //We shouldn't need to set a user agent, but it's here just in case
        //connection.setRequestProperty("User-Agent", null);

        //Try make the connection, it will hang here if the connection is bad
        connection.connect();

        int code = connection.getResponseCode();

        if ((code < 200 || code > 299) && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            //Didn't get what we expected
            throw new IOException(connection.getResponseMessage() + " for " + from);
        }

        long modifyTime = connection.getHeaderFieldDate("Last-Modified", -1);

        if (to.exists() && (code == HttpURLConnection.HTTP_NOT_MODIFIED || modifyTime > 0 && to.lastModified() >= modifyTime)) {
            if (!quiet) {
                logger.info("'{}' Not Modified, skipping.", to);
            }

            return false; //What we've got is already fine
        }

        long contentLength = connection.getContentLengthLong();

        if (!quiet && contentLength >= 0) {
            logger.info("'{}' Changed, downloading {}", to, toNiceSize(contentLength));
        }

        try { //Try download to the output
            InputStream in = connection.getInputStream();

            if ("gzip".equals(connection.getContentEncoding())) {
                in = new GZIPInputStream(in);
            }

            FileUtils.copyInputStreamToFile(in, to);
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            to.delete(); //Probably isn't good if it fails to copy/save
            throw e;
        }

        //Set the modify time to match the server's (if we know it)
        if (modifyTime > 0) {
            //noinspection ResultOfMethodCallIgnored
            to.setLastModified(modifyTime);
        }

        //Save the ETag (if we know it)
        String eTag = connection.getHeaderField("ETag");

        if (eTag != null) {
            //Log if we get a weak ETag and we're not on quiet
            if (!quiet && eTag.startsWith("W/")) {
                logger.warn("Weak ETag found.");
            }

            saveETag(to, eTag, logger);
        }
        return true;
    }

    /**
     * Creates a new file in the same directory as the given file with <code>.etag</code> on the end of the name.
     *
     * @param file The file to produce the ETag for
     * @return The (uncreated) ETag file for the given file
     */
    private static File getETagFile(File file) {
        return new File(file.getAbsoluteFile().getParentFile(), file.getName() + ".etag");
    }

    /**
     * Attempt to load an ETag for the given file, if it exists.
     *
     * @param to The file to load an ETag for
     * @param logger The logger to print errors to if it goes wrong
     * @return The ETag for the given file, or <code>null</code> if it doesn't exist
     */
    private static String loadETag(File to, Logger logger) {
        File eTagFile = getETagFile(to);

        if (!eTagFile.exists()) {
            return null;
        }

        try {
            return Files.asCharSource(eTagFile, StandardCharsets.UTF_8).read();
        } catch (IOException e) {
            logger.warn("Error reading ETag file '{}'.", eTagFile);
            return null;
        }
    }

    /**
     * Saves the given ETag for the given file, replacing it if it already exists.
     *
     * @param to The file to save the ETag for
     * @param eTag The ETag to be saved
     * @param logger The logger to print errors to if it goes wrong
     */
    private static void saveETag(File to, String eTag, Logger logger) {
        File eTagFile = getETagFile(to);

        try {
            if (!eTagFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                eTagFile.createNewFile();
            }

            Files.asCharSink(eTagFile, StandardCharsets.UTF_8).write(eTag);
        } catch (IOException e) {
            logger.warn("Error saving ETag file '{}'.", eTagFile, e);
        }
    }

    /**
     * Format the given number of bytes as a more human readable string.
     *
     * @param bytes The number of bytes
     * @return The given number of bytes formatted to kilobytes, megabytes or gigabytes if appropriate
     */
    private static String toNiceSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return bytes / 1024 + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static String mapClassName(String obfuscated) {
        return FabricLoader.getInstance().isDevelopmentEnvironment() ? FabricLoader.getInstance().getMappingResolver().mapClassName("official", obfuscated) : obfuscated;
    }

    public static String mapFieldName(String className, String obfuscated, String descriptor) {
        return FabricLoader.getInstance().isDevelopmentEnvironment() ? FabricLoader.getInstance().getMappingResolver().mapFieldName("official", className, obfuscated, descriptor) : obfuscated;
    }
}
