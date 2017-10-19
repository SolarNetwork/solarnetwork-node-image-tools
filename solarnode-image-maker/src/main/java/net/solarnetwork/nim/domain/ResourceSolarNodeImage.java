/* ==================================================================
 * ResourceSolarNodeImage.java - 18/10/2017 10:47:29 AM
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

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A {@link Resource} based node image.
 * 
 * @author matt
 * @version 1.0
 */
public class ResourceSolarNodeImage implements SolarNodeImage {

  private final SolarNodeImageInfo info;
  private final Resource imageResource;

  /**
   * Constructor.
   * 
   * @param info
   *          the info
   * @param imageResource
   *          the image resource data
   */
  public ResourceSolarNodeImage(SolarNodeImageInfo info, Resource imageResource) {
    super();
    this.info = info;
    this.imageResource = imageResource;
  }

  @Override
  public String getId() {
    return info.getId();
  }

  @Override
  public String getSha256() {
    return info.getSha256();
  }

  @Override
  public String getUncompressedSha256() {
    return info.getUncompressedSha256();
  }

  @JsonIgnore
  @Override
  public InputStream getInputStream() throws IOException {
    return imageResource.getInputStream();
  }

  @Override
  public String getFilename() {
    return imageResource.getFilename();
  }

  @Override
  public long contentLength() {
    try {
      return imageResource.contentLength();
    } catch (IOException e) {
      return 0;
    }
  }

  /**
   * Get the source info instance.
   * 
   * @return the info
   */
  @JsonIgnore
  public SolarNodeImageInfo getInfo() {
    return info;
  }

  /**
   * Get the source resource instance.
   * 
   * @return the resource
   */
  @JsonIgnore
  public Resource getImageResource() {
    return imageResource;
  }

}
