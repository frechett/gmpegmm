package com.isti.gmpegmm;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.logging.LogManager;

import gov.usgs.earthquake.nshmp.Earthquakes;
import gov.usgs.earthquake.nshmp.Maths;
import gov.usgs.earthquake.nshmp.geo.Coordinates;
import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.geo.Locations;
import gov.usgs.earthquake.nshmp.internal.Logging;

/**
 * Static utility methods.
 */
public class UtilFns {
    /** Character set */
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    /** Default buffer size */
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    /** Empty string. */
    public static final String EMPTY_STRING = "";
    /** The resource directory */
    public static final String RESOURCE_DIR;
    /** The GmpeGmm resource directory key */
    public static final String RESOURCE_DIR_KEY = "GMPEGMM_RESOURCE_DIR";
    /** The resource directory name */
    public static final String RESOURCE_DIRNAME = "res/";
    /** The separator character */
    public static final char SEPARATOR_CHAR = '/';
    static {
        RESOURCE_DIR = System.getProperty(RESOURCE_DIR_KEY, RESOURCE_DIRNAME);
    }

    /**
     * Calculate the distance.
     * 
     * @param p1 the first position.
     * @param p2 the second position.
     * @return the distance.
     */
    public static double calcDistance(Location p1, Location p2) {
        return Locations.horzDistanceFast(p1, p2);
    }

    /**
     * Calculate the distance to rupture plane.
     * 
     * @param distance the distance.
     * @param depth    the depth.
     * @return the rupture distance.
     */
    public static double calcDistanceToRupture(double distance, double depth) {
        return Maths.hypot(distance, depth);
    }

    /**
     * Ensure {@code -5 ≤ depth ≤ 700 km}.
     * 
     * @param depth to validate
     * @return the validated depth
     * @throws IllegalArgumentException if {@code depth} is outside the range
     *                                  {@code [-5..700] km}
     */
    public static double checkDepth(double depth) {
        return Earthquakes.checkDepth(depth);
    }

    /**
     * Ensure {@code -5 ≤ depth ≤ 700 km}.
     * 
     * @param depth to validate
     * @return the validated depth
     * @throws IllegalArgumentException if {@code depth} is outside the range
     *                                  {@code [-5..700] km}
     */
    public static double checkDepth(String depth) {
        return checkDepth(parseDouble(depth));
    }

    /**
     * Ensure that {@code -90° ≤ latitude ≤ 90°}.
     *
     * @param latitude to validate
     * @return the validated latitude
     * @throws IllegalArgumentException if {@code latitude} is outside the range
     *                                  {@code [-90..90]°}
     */
    public static double checkLatitude(double latitude) {
        return Coordinates.checkLatitude(latitude);
    }

    /**
     * Ensure that {@code -90° ≤ latitude ≤ 90°}.
     *
     * @param latitude to validate
     * @return the validated latitude
     * @throws IllegalArgumentException if {@code latitude} is outside the range
     *                                  {@code [-90..90]°}
     */
    public static double checkLatitude(String latitude) {
        return checkLatitude(parseDouble(latitude));
    }

    /**
     * Ensure that {@code -360° < longitude < 360°}.
     *
     * @param longitude to validate
     * @return the validated longitude
     * @throws IllegalArgumentException if {@code longitude} is outside the range
     *                                  {@code (-360..360)°}
     */
    public static double checkLongitude(double longitude) {
        return Coordinates.checkLongitude(longitude);
    }

    /**
     * Ensure that {@code -360° < longitude < 360°}.
     *
     * @param longitude to validate
     * @return the validated longitude
     * @throws IllegalArgumentException if {@code longitude} is outside the range
     *                                  {@code (-360..360)°}
     */
    public static double checkLongitude(String longitude) {
        return checkLongitude(parseDouble(longitude));
    }

    /**
     * Ensure {@code -2.0 ≤ magnitude ≤ 9.7}.
     *
     * @param magnitude to validate
     * @return the validated magnitude
     * @throws IllegalArgumentException if {@code magnitude} value is outside the
     *                                  range {@code [-2.0..9.7]}
     */
    public static double checkMagnitude(double magnitude) {
        return Earthquakes.checkMagnitude(magnitude);
    }

    /**
     * Ensure {@code -2.0 ≤ magnitude ≤ 9.7}.
     *
     * @param magnitude to validate
     * @return the validated magnitude
     * @throws IllegalArgumentException if {@code magnitude} value is outside the
     *                                  range {@code [-2.0..9.7]}
     */
    public static double checkMagnitude(String magnitude) {
        return checkMagnitude(parseDouble(magnitude));
    }

    /**
     * Close the source quietly ignoring any exceptions.
     * 
     * @param closeable the source to close.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Copy the input stream to the output file.
     * 
     * @param output    the output file.
     * @param in        the input stream.
     * @param overwrite true to overwrite existing file, false otherwise.
     * @throws FileNotFoundException - if the file exists and overwrite was not
     *                               specified, if the file exists but is a
     *                               directory rather than a regular file, does not
     *                               exist but cannot be created, or cannot be
     *                               opened for any other reason.
     * @throws IOException           if an I/O error occurs.
     */
    public static void copy(final File output, final InputStream in, final boolean overwrite) throws IOException {
        OutputStream out = null;
        try {
            if (output.exists()) {
                if (output.isDirectory()) {
                    throw new FileNotFoundException("directory exists with file name (" + output + ")");
                }
                if (!overwrite) {
                    throw new FileNotFoundException("file already exists (" + output + ")");
                }
            }
            File dir = output.getParentFile();
            if (dir != null) {
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        throw new FileNotFoundException("cannot create directory (" + dir + ")");
                    }
                } else if (dir.isFile()) {
                    throw new FileNotFoundException("file exists with directory name (" + dir + ")");
                } else if (!dir.canWrite()) {
                    throw new FileNotFoundException("cannot write to directory (" + dir + ")");
                }
            }
            out = new FileOutputStream(output);
            long count = UtilFns.transfer(out, in);
            if (count == 0) {
                throw new IOException("could not copy file (" + output + ")");
            }
        } finally {
            UtilFns.closeQuietly(out);
        }
    }

    /**
     * Create the location.
     * 
     * @param lat the latitude.
     * @param lon the longitude.
     * @return the location.
     */
    public static Location createLocation(double lat, double lon) {
        return createLocation(lat, lon, 0.0);
    }

    /**
     * Create the location.
     * 
     * @param lat   the latitude.
     * @param lon   the longitude.
     * @param depth the depth.
     * @return the location.
     */
    public static Location createLocation(double lat, double lon, double depth) {
        return Location.create(lon, lat, depth);
    }

    /**
     * Delete the directory and all of it's children.
     * 
     * @param directory the directory.
     */
    public static void delete(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (Exception ex) {
        }
    }

    /** @return the maximum depth */
    public static double getDepthMax() {
        return Earthquakes.DEPTH_RANGE.upperEndpoint();
    }

    /** @return the minimum depth */
    public static double getDepthMin() {
        return Earthquakes.DEPTH_RANGE.lowerEndpoint();
    }

    /** @return the maximum latitude */
    public static double getLatitudeMax() {
        return Coordinates.LAT_RANGE.upperEndpoint();
    }

    /** @return the minimum latitude */
    public static double getLatitudeMin() {
        return Coordinates.LAT_RANGE.lowerEndpoint();
    }

    /**
     * Get the log prefix for the specified class.
     * 
     * @param c the class.
     * @return the log prefix.
     */
    public static String getLogPrefix(Class<?> c) {
        return c.getSimpleName() + ": ";
    }

    /** @return the maximum longitude */
    public static double getLongitudeMax() {
        return Coordinates.LON_RANGE.upperEndpoint();
    }

    /** @return the minimum longitude */
    public static double getLongitudeMin() {
        return Coordinates.LON_RANGE.lowerEndpoint();
    }

    /** @return the maximum magnitude */
    public static double getMagnitudeMax() {
        return Earthquakes.MAG_RANGE.upperEndpoint();
    }

    /** @return the minimum magnitude */
    public static double getMagnitudeMin() {
        return Earthquakes.MAG_RANGE.lowerEndpoint();
    }

    /**
     * Get the name.
     * 
     * @param names the names.
     * @return the name.
     */
    public static String getName(String... names) {
        if (names == null || names.length == 0) {
            return EMPTY_STRING;
        }
        if (names.length == 1) {
            return names[0];
        }
        final StringBuilder sb = new StringBuilder(names[0]);
        for (int i = 1; i < names.length; i++) {
            if (sb.charAt(sb.length() - 1) != SEPARATOR_CHAR) {
                sb.append(SEPARATOR_CHAR);
            }
            sb.append(names[i]);
        }
        return sb.toString();
    }

    /**
     * Initialize logging from {@code logging.properties}.
     */
    public static void loggingInit() {
        InputStream is = null;
        try {
            File file = new File("lib/logging.properties");
            if (file.canRead()) {
                is = new FileInputStream(file);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (is == null) {
            try {
                is = Logging.class.getResourceAsStream("/logging.properties");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (is != null) {
            try {
                LogManager.getLogManager().readConfiguration(is);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        UtilFns.closeQuietly(is);
    }

    /**
     * Open the input stream for the specified name.
     * 
     * @param name the file name or system resource name.
     * @return the input stream, not null.
     * @throws FileNotFoundException if the input stream could not be opened.
     */
    public static InputStream openInputStream(String name) throws FileNotFoundException {
        File file = null;
        file = new File(name);
        try {
            if (file.exists()) {
                return new FileInputStream(file);
            }
        } catch (Exception ex) {
        }
        if (name.startsWith(RESOURCE_DIR)) {
            if (!RESOURCE_DIR.equals(RESOURCE_DIRNAME)) {
                file = new File(RESOURCE_DIRNAME, name.substring(RESOURCE_DIR.length()));
                try {
                    if (file.exists()) {
                        return new FileInputStream(file);
                    }
                } catch (Exception ex) {
                }
            }
        } else if (name.startsWith(RESOURCE_DIRNAME)) {
            if (!RESOURCE_DIR.equals(RESOURCE_DIRNAME)) {
                file = new File(RESOURCE_DIR, name.substring(RESOURCE_DIRNAME.length()));
                try {
                    if (file.exists()) {
                        return new FileInputStream(file);
                    }
                } catch (Exception ex) {
                }
            }
        } else {
            file = new File(RESOURCE_DIR, name);
            try {
                if (file.exists()) {
                    return new FileInputStream(file);
                }
            } catch (Exception ex) {
            }
            file = new File(RESOURCE_DIRNAME, name);
            try {
                if (file.exists()) {
                    return new FileInputStream(file);
                }
            } catch (Exception ex) {
            }
        }
        InputStream inputStream = openResourceInputStream(name);
        if (inputStream == null) {
            throw new FileNotFoundException("Could not open input stream (" + name + ")");
        }
        return inputStream;
    }

    /**
     * Open the input stream for a resource with the specified name.
     * 
     * @param name the system resource name.
     * @return the input stream or null if none.
     */
    public static InputStream openResourceInputStream(String name) {
        if (!name.startsWith(RESOURCE_DIRNAME)) {
            name = RESOURCE_DIRNAME + name;
        }
        return ClassLoader.getSystemResourceAsStream(name);
    }

    /**
     * Parse the text for a double.
     * 
     * @param s the text.
     * @return the double.
     * @throws IllegalArgumentException if the text is illegal.
     */
    public static double parseDouble(String s) throws IllegalArgumentException {
        try {
            return Double.parseDouble(s);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal double value (" + s + ")");
        }
    }

    /**
     * Read all of the characters from the input stream.
     * 
     * @param in the input stream.
     * @return a string containing all of the characters from the input stream.
     * @throws IOException
     */
    public static String readFully(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        transfer(out, in);
        return new String(out.toByteArray(), CHARSET);
    }

    /**
     * Reads all bytes from the given input stream and writes the bytes to the given
     * output stream in the order that they are read. On return, this input stream
     * will be at end of stream. This method does not close either stream.
     * <p>
     * This method may block indefinitely reading from the input stream, or writing
     * to the output stream. The behavior for the case where the input and/or output
     * stream is <i>asynchronously closed</i>, or the thread interrupted during the
     * transfer, is highly input and output stream specific, and therefore not
     * specified.
     * <p>
     * If an I/O error occurs reading from the input stream or writing to the output
     * stream, then it may do so after some bytes have been read or written.
     * Consequently the input stream may not be at end of stream and one, or both,
     * streams may be in an inconsistent state. It is strongly recommended that both
     * streams be promptly closed if an I/O error occurs.
     *
     * @param out the output stream, non-null
     * @param in  the input stream, non-null
     * @return the number of bytes transferred
     * @throws IOException          if an I/O error occurs when reading or writing
     * @throws NullPointerException if {@code in} or {@code out} is {@code null}
     */
    public static long transfer(OutputStream out, InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }
}
