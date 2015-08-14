
package osm.custommaps.kml;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

import osm.custommaps.ImageHelper;

/**
 * KmlFile provides methods to read kml files.
 *
 * @author Marko Teittinen
 */
public class KmlFile implements KmlInfo, Serializable {
  private static final long serialVersionUID = 1L;

  private File kmlFile;

  public KmlFile(File kmlFile) {
    this.kmlFile = kmlFile;
  }

  public File getFile() {
    return kmlFile;
  }

  public Reader getKmlReader() throws IOException {
    return new FileReader(kmlFile);
  }

  public long getImageDate(String path) {
    File imageFile = new File(kmlFile.getParentFile(), path);
    return imageFile.lastModified();
  }

  public InputStream getImageStream(String path) throws IOException {
    File imageFile = new File(kmlFile.getParentFile(), path);
    return new FileInputStream(imageFile);
  }

  public int getImageOrientation(String path) {
    String imageFilename = new File(kmlFile.getParentFile(), path).getAbsolutePath();
    return ImageHelper.readOrientation(imageFilename);
  }

  @Override
  public String toString() {
    return "KmlFile[path='" + kmlFile.getAbsolutePath() + "']";
  }
}
