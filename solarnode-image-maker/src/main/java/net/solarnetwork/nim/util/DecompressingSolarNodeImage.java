/* ==================================================================
 * DecompressingSolarNodeImage.java - 22/10/2017 3:44:13 PM
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
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import net.solarnetwork.nim.domain.SolarNodeImage;

/**
 * {@link SolarNodeImage} that delegates to another instance and decompresses that instance's
 * {@link SolarNodeImage#getInputStream()}.
 * 
 * @author matt
 * @version 1.0
 */
public class DecompressingSolarNodeImage implements SolarNodeImage {

  private final SolarNodeImage delegate;
  private final String compressionType;

  /**
   * Construct with auto-detecting compression type.
   * 
   * @param delegate
   *          the delgate
   */
  public DecompressingSolarNodeImage(SolarNodeImage delegate) {
    this(delegate, null);
  }

  /**
   * Construct with a specific compression type.
   * 
   * @param delegate
   *          the delgate
   * @param compressionType
   *          the compression type
   */
  public DecompressingSolarNodeImage(SolarNodeImage delegate, String compressionType) {
    super();
    this.delegate = delegate;
    this.compressionType = compressionType;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    InputStream in = new BufferedInputStream(delegate.getInputStream());
    String type = compressionType;
    try {
      if (type == null) {
        type = CompressorStreamFactory.detect(in);
      }
      return new CompressorStreamFactory().createCompressorInputStream(type, in);
    } catch (CompressorException e) {
      throw new IOException(
          "Error handling compression of image " + getId() + ": " + e.getMessage());
    }
  }

  @Override
  public String getSha256() {
    return delegate.getSha256();
  }

  @Override
  public long getContentLength() {
    return delegate.getContentLength();
  }

  @Override
  public String getFilename() {
    return delegate.getFilename();
  }

  @Override
  public String getUncompressedSha256() {
    return delegate.getUncompressedSha256();
  }

  @Override
  public long getUncompressedContentLength() {
    return delegate.getUncompressedContentLength();
  }

}
