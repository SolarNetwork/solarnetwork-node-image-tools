/* ==================================================================
 * CompressingResource.java - 19/10/2017 10:53:13 AM
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
 * A {@link Resource} that compresses another {@link Resource}.
 * 
 * <p>
 * This implementation uses the Apache Commons Compression library to return compressing
 * {@link InputStream} instances from {@link #getInputStream()}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CompressingResource extends AbstractResource {

  private final Resource source;
  private final String compressionType;

  /**
   * Constructor.
   * 
   * @param source
   *          the source (compressed) resource
   * @param compressionType
   *          the {@link CompressorStreamFactory} compression type to use
   */
  public CompressingResource(Resource source, String compressionType) {
    super();
    this.source = source;
    this.compressionType = compressionType;
  }

  @Override
  public String getDescription() {
    return "CompressingResource{source=" + source + ",compressionType=" + compressionType + "}";
  }

  @Override
  public InputStream getInputStream() throws IOException {
    BufferedInputStream in = new BufferedInputStream(source.getInputStream());
    try {
      return new CompressorStreamFactory().createCompressorInputStream(compressionType, in);
    } catch (CompressorException e) {
      throw new IOException("Error handling " + compressionType + " compression of image " + source
          + ": " + e.getMessage());
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

  @Override
  public String getFilename() {
    return super.getFilename() + "." + compressionType;
  }

  /**
   * Get the configured compression type.
   * 
   * @return the configured compression type
   */
  public String getCompressionType() {
    return compressionType;
  }

}
