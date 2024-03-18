package com.isti.gmpegmm;

/** Region */
public enum Region {
    // label, minlatitude, maxlatitude, minlongitude, maxlongitude
    /** Alaska */
    AK("Alaska", 48.0, 72.0, -200.0, -125.0),
    /** Central & Eastern US */
    CEUS("Central & Eastern US", 24.6, 50.0, -115.0, -65.0),
    /** Conterminous US */
    COUS("Conterminous US", 24.6, 50.0, -125.0, -65.0),
    /** Global */
    GLOBAL("Global", -90.0, 90.0, -180.0, 180.0),
    /** Western US */
    WUS("Western US", 24.6, 50.0, -125.0, -100.0);

    /**
     * Check if the longitude in the Conterminous US is in the Western US or in the
     * Central & Eastern US.
     * 
     * @param lon the longitude.
     * @return {@link #WUS} if the longitude is in the Western US, {@link #CEUS} if
     *         the longitude is in the Central & Eastern US or {@link #COUS} if the
     *         longitude is in both the Western US and the the Central & Eastern US.
     */
    public static Region checkRegion(double lon) {
        return (lon <= CEUS.minlongitude) ? WUS : (lon >= WUS.maxlongitude) ? CEUS : COUS;
    }

    /**
     * Get the region for the specified latitude and longitude.
     * 
     * @param lat the longitude.
     * @param lon the longitude.
     * @return the region.
     */
    public static Region getRegion(double lat, double lon) {
        // if Conterminous US
        if (COUS.contains(lat, lon)) {
            return checkRegion(lon);
        }
        if (AK.contains(lat, lon)) {
            return AK;
        }
        return GLOBAL;
    }

    private final String label;
    private final double maxlatitude;
    private final double maxlongitude;
    private final double minlatitude;
    private final double minlongitude;

    /**
     * Create the region.
     * 
     * @param label        the label.
     * @param minlatitude  the minimum latitude.
     * @param maxlatitude  the maximum latitude.
     * @param minlongitude the minimum longitude.
     * @param maxlongitude the maximum longitude.
     */
    private Region(String label, double minlatitude, double maxlatitude, double minlongitude, double maxlongitude) {
        this.label = label;
        this.minlatitude = minlatitude;
        this.maxlatitude = maxlatitude;
        this.minlongitude = minlongitude;
        this.maxlongitude = maxlongitude;
    }

    public boolean contains(double lat, double lon) {
        return lon >= minlongitude && lon <= maxlongitude && lat >= minlatitude && lat <= maxlatitude;
    }

    /** @return the label */
    public String getLabel() {
        return label;
    }

    /** @return the maximum latitude */
    public double getMaxlatitude() {
        return maxlatitude;
    }

    /** @return the maximum longitude */
    public double getMaxlongitude() {
        return maxlongitude;
    }

    /** @return the minimum latitude */
    public double getMinlatitude() {
        return minlatitude;
    }

    /** @return the minimum longitude */
    public double getMinlongitude() {
        return minlongitude;
    }
}
