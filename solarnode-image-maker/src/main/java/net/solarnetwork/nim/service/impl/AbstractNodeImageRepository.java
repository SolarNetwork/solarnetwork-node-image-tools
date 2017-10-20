/* ==================================================================
 * AbstractNodeImageRepository.java - 21/10/2017 11:53:03 AM
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

package net.solarnetwork.nim.service.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.util.MaxCompressorStreamFactory;

/**
 * Supporting base class for {@link NodeImageRepository} implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractNodeImageRepository implements NodeImageRepository {

  /**
   * A mapper to use for dealing with JSON metadata.
   */
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private String compressionType = "xz";
  private float compressionRatio = 1f;

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Create a compressing output stream.
   * 
   * @param out
   *          the output stream to write to
   * @return the output stream
   * @throws CompressorException
   *           if an error configuring compression occurs
   * @throws IOException
   *           if an IO error occurs
   */
  protected OutputStream createCompressorOutputStream(OutputStream out)
      throws CompressorException, IOException {
    CompressorStreamFactory compressorFactory = new MaxCompressorStreamFactory(compressionRatio);
    return compressorFactory.createCompressorOutputStream(compressionType, out);
  }

  /**
   * Set the Apache Commons Compression library compression type compress images with.
   * 
   * @param compressionType
   *          the compression type; defaults to {@literal xz}
   */
  public void setCompressionType(String compressionType) {
    this.compressionType = compressionType;
  }

  /**
   * Get the compression type.
   * 
   * @return the compression type; defaults to {@literal xz}
   */
  public String getCompressionType() {
    return compressionType;
  }

  /**
   * Set the desired compression ratio to use when compressing images.
   * 
   * @param compressionRatio
   *          a ratio between {@literal 0} (least compression) and {@literal 1} (higest compression)
   * @see net.solarnetwork.nim.util.MaxCompressorStreamFactory#MaxCompressorStreamFactory(float)
   */
  public void setCompressionRatio(float compressionRatio) {
    this.compressionRatio = compressionRatio;
  }

  /**
   * Get the desired compression ratio to use when compressing images.
   * 
   * @return a ratio between {@literal 0} (least compression) and {@literal 1} (higest compression)
   */
  public float getCompressionRatio() {
    return compressionRatio;
  }

}
