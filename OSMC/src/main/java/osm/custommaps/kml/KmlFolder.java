
package osm.custommaps.kml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * KmlFolder manages a set of KmlFeatures stored in kml and kmz files located
 * within a single source directory (folder). Supported KmlFeatures are
 * GroundOverlay and Placemarks.
 *
 * @author Marko Teittinen
 */
public class KmlFolder extends KmlFeature {
  static final long serialVersionUID = 1L;

  private List<KmlFeature> features = new ArrayList<KmlFeature>();

  /**
   * Adds a GroundOverlay or a Placemark to this folder
   *
   * @param feature GroundOverlay (map) or a Placemark (icon)
   */
  public void addFeature(KmlFeature feature) {
    features.add(feature);
  }

  /**
   * Adds all KmlFeatures from a Collection to this KmlFolder.
   *
   * @param moreFeatures features to be added to this folder
   */
  public void addFeatures(Collection<? extends KmlFeature> moreFeatures) {
    if (moreFeatures != null) {
      features.addAll(moreFeatures);
    }
  }

  /**
   * @return true, if this folder contains any KmlFeatures
   */
  public boolean hasFeatures() {
    return !features.isEmpty();
  }

  /**
   * @return true, if this folder contains any Placemarks
   */
  public boolean hasPlacemarks() {
    for (KmlFeature feature : features) {
      if (feature instanceof Placemark) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return Iterable over all KmlFeatures stored in this folder
   */
  public Iterable<KmlFeature> getFeatures() {
    return features;
  }

  /**
   * @return The first GroundOverlay found in this folder
   */
  public GroundOverlay getFirstMap() {
    for (KmlFeature feature : features) {
      if (feature instanceof GroundOverlay) {
        return (GroundOverlay) feature;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof KmlFolder)) {
      return false;
    }
    KmlFolder other = (KmlFolder) obj;
    // KmlFolders are considered equal if their files match and their map names match
    String myName = this.getName();
    String otherName = other.getName();
    File myFile = (this.getKmlInfo() != null ? this.getKmlInfo().getFile() : null);
    File otherFile = (other.getKmlInfo() != null ? other.getKmlInfo().getFile() : null);
    String myFileName = (myFile != null ? myFile.getAbsolutePath() : null);
    String otherFileName = (otherFile != null ? otherFile.getAbsolutePath() : null);
    return (myName == otherName || (myName != null && myName.equals(otherName))) &&
        (myFileName == otherFileName || (myFileName != null && myFileName.equals(otherFileName)));
  }
}
