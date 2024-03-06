package gov.usgs.earthquake.nshmp.eq.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.isti.gmpegmm.UtilFns;

import gov.usgs.earthquake.nshmp.data.Data;
import gov.usgs.earthquake.nshmp.gmm.Gmm;
import gov.usgs.earthquake.nshmp.gmm.GmmAttribute;
import gov.usgs.earthquake.nshmp.gmm.GmmElement;
import gov.usgs.earthquake.nshmp.gmm.GroundMotionModel;
import gov.usgs.earthquake.nshmp.internal.Parsing;

/*
 * Non-validating gmm.xml parser. SAX parser 'Attributes' are stateful and
 * cannot be stored. This class is not thread safe.
 *
 * This is based on the GmmParser but adds support for regions.
 *
 * @author ISTI
 * @see #GmmParser
 */
public class GmmsParser extends DefaultHandler {
  /** The GMM file name */
  public static final String GMM_FILENAME = "gmm.xml";

  private static final Logger log = Logger
      .getLogger(GmmsParser.class.getName());

  private static GmmsParser create(SAXParser sax) {
    return new GmmsParser(checkNotNull(sax));
  }

  /**
   * Get the GMM weight map.
   * 
   * @param region   the region.
   * @param distance the distance.
   * @param input    the input for the GMM XML file.
   * @return the GMM weight map.
   */
  public static Map<Gmm, Double> gmmWeightMap(String region, double distance,
      InputStream input) {
    final GmmsParser gmms;
    try {
      final SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
      int mapCount;
      gmms = create(sax);
      gmms.setRegion(region);
      mapCount = gmms.parse(input);
      if (mapCount == 0) {
        log.warning(region + ": no map found");
      } else {
        log.fine(region + ": map count is " + mapCount);
      }
    } catch (Exception ex) {
      log.warning("Could not get weight map for " + region + " [distance = "
          + distance + "]: " + ex.toString());
      return Collections.emptyMap();
    }
    return gmms.gmmWeightMap(distance);
  }

  /**
   * Test program.
   * 
   * @param args the program arguments.
   */
  public static void main(String[] args) {
    InputStream in = null;
    try {
      int mapCount;
      SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
      String[] regions;
      if (args.length > 0) {
        regions = args;
      } else {
        regions = new String[] { "CEUS", "WUS", "COUS" };
      }
      for (String region : regions) {
        in = UtilFns.openInputStream(GMM_FILENAME);
        GmmsParser gmms = create(sax);
        gmms.setRegion(region);
        if ((mapCount = gmms.parse(in)) == 0) {
          log.warning(region + ": no map found");
        } else {
          log.info(region + ": map count is " + mapCount);
        }
        in.close();
        in = null;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  private GmmSet gmmSet;
  private Map<Gmm, Double> gmmWtMap;
  private Locator locator;
  private int mapCount = 0;
  private double maxDistance;
  private String region;
  private final SAXParser sax;
  private GmmSet.Builder setBuilder;
  private boolean used = false;

  private GmmsParser(SAXParser sax) {
    this.sax = sax;
  }

  @Override
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
    // ignore if no set builder
    if (setBuilder == null) {
      return;
    }
    GmmElement e = null;
    try {
      e = GmmElement.fromString(qName);
    } catch (IllegalArgumentException iae) {
      throw new SAXParseException("Invalid element <" + qName + ">", locator,
          iae);
    }

    try {
      switch (e) {
      case MODEL_SET:
        // ignore if no weight map
        if (gmmWtMap == null) {
          break;
        }
        if (mapCount == 1) {
          setBuilder.primaryModelMap(gmmWtMap);
        } else {
          setBuilder.secondaryModelMap(gmmWtMap);
        }
        Data.checkWeights(gmmWtMap.values());
        break;
      case GROUND_MOTION_MODELS:
        // ignore if no set builder
        if (setBuilder == null) {
          return;
        }
        gmmSet = setBuilder.build();
        setBuilder = null;
        gmmWtMap = null;
        break;
      case MODEL:
      case UNCERTAINTY:
        break;
      }
    } catch (Exception ex) {
      throw new SAXParseException("Error parsing <" + qName + ">", locator, ex);
    }
  }

  /**
   * @return the region.
   */
  public String getRegion() {
    return region;
  }

  /**
   * The {@code Map} of {@link GroundMotionModel} identifiers and associated
   * weights to use at a given {@code distance} from a {@code Site}.
   * 
   * @param distance the distance.
   * @return the GMM weight map.
   */
  public Map<Gmm, Double> gmmWeightMap(double distance) {
    if (gmmSet == null) {
      return Collections.emptyMap();
    }
    return gmmSet.gmmWeightMap(distance);
  }

  /**
   * Parse the input.
   * 
   * @param in the input.
   * @return the map count or 0 if none.
   * @throws SAXException if any SAX errors occur.
   * @throws IOException  if any IO errors occur.
   */
  public int parse(InputStream in) throws SAXException, IOException {
    checkState(!used, "This parser has expired");
    sax.parse(checkNotNull(in), this);
    used = true;
    return mapCount;
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  /**
   * Set the region.
   * 
   * @param region the region.
   */
  public void setRegion(String region) {
    this.region = region;
  }

  @Override
  public void startElement(String uri, String localName, String qName,
      Attributes atts) throws SAXException {
    GmmElement e = null;
    try {
      e = GmmElement.fromString(qName);
    } catch (IllegalArgumentException iae) {
      throw new SAXParseException("Invalid element <" + qName + ">", locator,
          iae);
    }

    try {
      String s;
      switch (e) {
      case GROUND_MOTION_MODELS:
        setBuilder = new GmmSet.Builder();
        break;
      case UNCERTAINTY:
        // ignore if no set builder
        if (setBuilder == null) {
          return;
        }
        double[] uncValues = Parsing.readDoubleArray(GmmAttribute.VALUES, atts);
        double[] uncWeights = Parsing.readDoubleArray(GmmAttribute.WEIGHTS,
            atts);
        setBuilder.uncertainty(uncValues, uncWeights);
        log.fine("Uncertainty...");
        log.fine("     Values: " + Arrays.toString(uncValues));
        log.fine("    Weights: " + Arrays.toString(uncWeights));
        break;
      case MODEL_SET:
        gmmWtMap = null;
        // ignore if no set builder
        if (setBuilder == null) {
          return;
        }
        if (region != null) {
          s = atts.getValue(GmmAttribute.ID.toString());
          // ignore if not the correct region
          if (!region.equals(s)) {
            break;
          }
        }
        mapCount++;
        checkState(mapCount < 3,
            "Only two ground motion model sets are allowed");
        gmmWtMap = Maps.newEnumMap(Gmm.class);
        if (mapCount == 1) {
          maxDistance = Parsing.readDouble(GmmAttribute.MAX_DISTANCE, atts);
          setBuilder.primaryMaxDistance(maxDistance);
        } else {
          maxDistance = Parsing.readDouble(GmmAttribute.MAX_DISTANCE, atts);
          setBuilder.secondaryMaxDistance(maxDistance);
        }
        log.fine("        Set: " + mapCount + " [max distance = " + maxDistance
            + "]");
        break;
      case MODEL:
        // ignore if no set builder
        if (setBuilder == null) {
          return;
        }
        // ignore if no weight map
        if (gmmWtMap == null) {
          break;
        }
        Gmm model = Parsing.readEnum(GmmAttribute.ID, atts, Gmm.class);
        double weight = Parsing.readDouble(GmmAttribute.WEIGHT, atts);
        gmmWtMap.put(model, weight);
        Level level = Level.FINE;
        if (log.isLoggable(level)) {
          log.log(level,
              " Model [wt]: " + Strings.padEnd(model.toString(), 44, ' ') + " ["
                  + weight + "]");
        }
        break;
      }
    } catch (Exception ex) {
      throw new SAXParseException("Error parsing <" + qName + ">", locator, ex);
    }
  }
}
