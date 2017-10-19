/* ==================================================================
 * BasicSolarNodeImageInfo.java - 18/10/2017 9:02:36 AM
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic immutable implementation of {@link SolarNodeImageInfo}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicSolarNodeImageInfo implements SolarNodeImageInfo {

  private final String id;
  private final String sha256;
  private final String uncompressedSha256;

  /**
   * Constructor.
   * 
   * @param id
   *          the ID of the image
   * @param sha256Hex
   *          the hex-encoded SHA256 digest of the image content
   * @param uncompressedSha256Hex
   *          the hex-encoded SHA256 digest of the uncompressed image contentf
   */
  @JsonCreator
  public BasicSolarNodeImageInfo(@JsonProperty("id") String id,
      @JsonProperty(value = "sha256", required = false) String sha256Hex,
      @JsonProperty(value = "uncompressedSha256", required = false) String uncompressedSha256Hex) {
    super();
    this.id = id;
    this.sha256 = sha256Hex;
    this.uncompressedSha256 = uncompressedSha256Hex;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getSha256() {
    return sha256;
  }

  @Override
  public String getUncompressedSha256() {
    return uncompressedSha256;
  }

}
