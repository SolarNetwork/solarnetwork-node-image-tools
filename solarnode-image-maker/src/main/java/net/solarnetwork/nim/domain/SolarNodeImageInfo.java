/* ==================================================================
 * SolarNodeImageInfo.java - 18/10/2017 7:45:11 AM
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

package net.solarnetwork.nim.domain;

/**
 * Metadata about a SolarNode image.
 * 
 * @author matt
 * @version 1.0
 */
public interface SolarNodeImageInfo {

  /**
   * Get a unique identifier for this image.
   * 
   * @return the unique ID
   */
  String getId();

  /**
   * Get a hex-encoded SHA-256 digest of the image content.
   * 
   * @return the SHA-256 digest, or {@literal null} if not known
   */
  String getSha256();

  /**
   * Get the content length of the image content, in bytes.
   * 
   * @return the size of the image
   */
  long getContentLength();

  /**
   * Get a hex-encoded SHA-256 digest of the image contents when uncompressed.
   * 
   * @return the SHA-256 digest, or {@literal null} if not known
   */
  String getUncompressedSha256();

  /**
   * Get the size of the image contents when uncompressed, in bytes.
   * 
   * @return the size of the uncompressed image
   */
  long getUncompressedContentLength();

}
