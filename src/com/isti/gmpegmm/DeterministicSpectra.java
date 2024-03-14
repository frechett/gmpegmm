package com.isti.gmpegmm;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import gov.usgs.earthquake.nshmp.calc.Site;
import gov.usgs.earthquake.nshmp.eq.model.GmmsParser;
import gov.usgs.earthquake.nshmp.geo.Location;
import gov.usgs.earthquake.nshmp.gmm.Gmm;
import gov.usgs.earthquake.nshmp.gmm.GmmInput;
import gov.usgs.earthquake.nshmp.gmm.GmmInput.Builder;
import gov.usgs.earthquake.nshmp.gmm.Imt;

/**
 * Calculates the PGA value using a deterministic response spectra. This is
 * based on the ResponseSpectra class.
 * 
 * @author kevin
 * @see #ResponseSpectra
 */
public class DeterministicSpectra {
  private static final String LOG_PREFIX = UtilFns
      .getLogPrefix(DeterministicSpectra.class);
  /** PGA Calculator command prefix */
  public static final String PGACALC_CMD_PREFIX = "java -cp GmpeGmm.jar ";
  /** PGA Calculator Default IMLS Key */
  public static final String PGACALC_DEFAULT_IMLS_KEY = "PGACALC_DEFAULT_IMLS";
  /** PGA Calculator Default IMLS Text */
  public static final String PGACALC_DEFAULT_IMLS_TEXT = System
      .getProperty(PGACALC_DEFAULT_IMLS_KEY, "0.001");
  /** PGA Calculator No Result Key */
  public static final String PGACALC_NO_RESULT_KEY = "PGACALC_NO_RESULT";
  /** PGA No Result Text or null if none */
  public static final String PGACALC_NO_RESULT_TEXT = System
      .getProperty(PGACALC_NO_RESULT_KEY);
  /** PGA Calculator output no delete key */
  public static final String PGACALC_OUTPUT_NODELETE_KEY = "PGACALC_OUTPUT_NODELETE";
  /** PGA Calculator output no delete text or null for default */
  public static final String PGACALC_OUTPUT_NODELETE_TEXT = System
      .getProperty(PGACALC_OUTPUT_NODELETE_KEY);
  /** PGA Calculator output path key */
  public static final String PGACALC_OUTPUT_PATH_KEY = "PGACALC_OUTPUT_PATH";
  /** PGA Calculator output path text or null for default */
  public static final String PGACALC_OUTPUT_PATH_TEXT = System
      .getProperty(PGACALC_OUTPUT_PATH_KEY);
  /** PGA Calculator Version */
  public static final String PGACALC_VERSION = "1.4.074";

  private static double calcPga(Logger log, Map<Gmm, Double> gmmWtMap,
      double mag, double rJB, double rx, double rRup, double vs30) {
    Gmm gmm;
    double weightTotal = 0;
    double weight;
    GmmInput input;
    double mean;
    double value = 0.;
    Iterator<Gmm> gmmIterator = gmmWtMap.keySet().iterator();
    Builder builder = GmmInput.builder().withDefaults();
    builder.mag(mag);
    builder.rJB(rJB);
    builder.rX(rx);
    builder.rRup(rRup);
    builder.vs30(vs30);
    input = builder.build();

    while (gmmIterator.hasNext()) {
      gmm = gmmIterator.next();
      weight = gmmWtMap.get(gmm);

      mean = gmm.instance(Imt.PGA).calc(input).mean();
      mean = Math.exp(mean);
      log.info(String.format("Gmm %s, Weight %f, Mean %.10f", gmm.name(),
          weight, mean));

      weightTotal += weight;
      value += mean * weight;
    }
    if (weightTotal != 0.0 && weightTotal != 1.0) {
      value /= weightTotal;
    }
    return value;
  }

  private static double calcPga(String[] args) throws FileNotFoundException {
    if (args.length < 7) {
      throw new IllegalArgumentException(
          "Invalid number of arguments: " + args.length);
    }
    final Logger log = Logger.getLogger(DeterministicSpectra.class.toString());
    double depth, lat, lon, rJB, rx;
    String siteName = args[0];
    String siteLonText = args[1];
    String siteLatText = args[2];
    String eqMagText = args[3];
    String eqLonText = args[4];
    String eqLatText = args[5];
    String eqDepthText = args[6];
    double vs30 = Site.VS_30_DEFAULT;
    if (args.length > 7) {
      try {
        vs30 = Double.parseDouble(args[7]);
      } catch (Exception ex) {
        throw new IllegalArgumentException(
            "invalid vs30 argument (" + args[7] + ")");
      }
    }

    double mag = UtilFns.checkMagnitude(eqMagText);
    lon = UtilFns.checkLongitude(siteLonText);
    lat = UtilFns.checkLatitude(siteLatText);
    Region region = Region.getRegion(lat, lon);
    switch (region) {
    case CEUS:
    case COUS:
    case WUS:
      break;
    default:
      throw new IllegalArgumentException("region is not supported: " + region);
    }

    Location siteLocation = UtilFns.createLocation(lat, lon);
    lon = UtilFns.checkLongitude(eqLonText);
    lat = UtilFns.checkLatitude(eqLatText);
    depth = UtilFns.checkDepth(eqDepthText);
    Location eqLocation = UtilFns.createLocation(lat, lon, depth);

    double distance = UtilFns.calcDistance(siteLocation, eqLocation);
    rJB = distance;
    rx = distance;
    double rRup = UtilFns.calcDistanceToRupture(distance, depth);
    final InputStream input = UtilFns.openInputStream(GmmsParser.GMM_FILENAME);
    Map<Gmm, Double> gmmWtMap = GmmsParser.gmmWeightMap(region.toString(),
        distance, input);
    log.info(String.format(
        "site=%s, region=%s, mag=%f, depth=%f, rJB=%f, rX=%f, rRup=%f, vs30=%f",
        siteName, region.toString(), mag, depth, rJB, rx, rRup, vs30));
    return calcPga(log, gmmWtMap, mag, rJB, rx, rRup, vs30);
  }

  /**
   * Run the program.
   * 
   * @param args the program arguments.
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage(System.err);
      return;
    }
    UtilFns.loggingInit();
    final Logger log = Logger.getLogger(DeterministicSpectra.class.toString());
    log.info(LOG_PREFIX + " v" + PGACALC_VERSION);
    String value = PGACALC_NO_RESULT_TEXT;
    try {
      value = String.format("%f", calcPga(args));
      log.info(String.format("%sPGA=%s", LOG_PREFIX, value));
    } catch (Exception ex) {
      log.warning(LOG_PREFIX + ex.toString());
      printUsage(System.err);
      System.exit(2);
      return;
    }
    System.out.println(value);
  }

  /**
   * Print the usage.
   * 
   * @param out the output.
   */
  public static void printUsage(Appendable out) {
    try {
      String cmdSuffix = DeterministicSpectra.class.getName()
          + " \"site name\" siteLon siteLat eqMag eqLon eqLat eqDepth [vs30]";
      String noResultText = "CHECK LOGFILE";
      out.append(DeterministicSpectra.class.getSimpleName());
      out.append(" v");
      out.append(PGACALC_VERSION);
      out.append("\n\n");
      out.append("Usage:\n");
      out.append(PGACALC_CMD_PREFIX);
      out.append(cmdSuffix);
      out.append(
          "\n\nWhere siteLon and eqLon are longitude values (decimal degrees between ");
      out.append(String.valueOf(UtilFns.getLongitudeMin()));
      out.append(" and ");
      out.append(String.valueOf(UtilFns.getLongitudeMax()));
      out.append("),\n"
          + "siteLat and eqLat are latitude values (decimal degrees between ");
      out.append(String.valueOf(UtilFns.getLatitudeMin()));
      out.append(" and ");
      out.append(String.valueOf(UtilFns.getLatitudeMax()));
      out.append("),\n" + "eqMag is the moment magnitude (Mw between ");
      out.append(String.valueOf(UtilFns.getMagnitudeMin()));
      out.append(" and ");
      out.append(String.valueOf(UtilFns.getMagnitudeMax()));
      out.append(") of the earthquake,\n"
          + "and eqDepth is the depth of the earthquake (kilometers between ");
      out.append(String.valueOf(UtilFns.getDepthMin()));
      out.append(" and ");
      out.append(String.valueOf(UtilFns.getDepthMax()));
      out.append("),\n" + "vs30 is the optional vs30 value (default is ");
      out.append(Double.toString(Site.VS_30_DEFAULT));
      out.append(").");
      out.append(
          "\n\nThe PGA value (g) is written to standard output on success. If there is an error the value specified\n"
              + "with the ");
      out.append(PGACALC_NO_RESULT_KEY);
      out.append(
          " property is written, otherwise nothing is written by default.\n\nFor example to write \"");
      out.append(noResultText);
      out.append(
          "\" if there is an error add the following to the command line:\n\n");
      out.append(PGACALC_CMD_PREFIX);
      out.append("-D");
      out.append(PGACALC_NO_RESULT_KEY);
      out.append("=\"");
      out.append(noResultText);
      out.append("\" ");
      out.append(cmdSuffix);
      out.append("\n");
    } catch (Exception ex) {
    }
  }
}
