/* ==================================================================
 * DecompressingResource.java - 18/10/2017 11:11:40 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.nim.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

/**
 * A {@link Resource} that decompresses another {@link Resource}.
 * 
 * <p>
 * This implementation uses the Apache Commons Compression library to return decompressing
 * {@link InputStream} instances from {@link #getInputStream()}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class DecompressingResource extends AbstractResource {

  private final Resource source;

  private String compressionType;

  /**
   * Constructor.
   * 
   * @param source
   *          the source (compressed) resource
   */
  public DecompressingResource(Resource source) {
    super();
    this.source = source;
  }

  @Override
  public String getDescription() {
    return "DecompressingResource{source=" + source + "}";
  }

  @Override
  public InputStream getInputStream() throws IOException {
    BufferedInputStream in = new BufferedInputStream(source.getInputStream());
    try {
      if (compressionType == null) {
        compressionType = CompressorStreamFactory.detect(in);
      }
      return new CompressorStreamFactory().createCompressorInputStream(compressionType, in);
    } catch (CompressorException e) {
      throw new IOException(
          "Error handling compression of image " + source + ": " + e.getMessage());
    }
  }

  @Override
  public URL getURL() throws IOException {
    return source.getURL();
  }

  @Override
  public File getFile() throws IOException {
    return source.getFile();
  }

  /**
   * Get the detected compression type.
   * 
   * @return the detected compression type
   */
  public String getCompressionType() {
    if (compressionType == null) {
      // call getInputStream for detection
      try (InputStream in = getInputStream()) {
        // nothing here
      } catch (IOException e) {
        // ignore here
      }
    }
    return compressionType;
  }

}
