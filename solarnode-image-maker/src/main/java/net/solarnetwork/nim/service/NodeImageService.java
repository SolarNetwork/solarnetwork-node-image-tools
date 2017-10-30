/* ==================================================================
 * NodeImageService.java - 18/10/2017 7:17:02 PM
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

package net.solarnetwork.nim.service;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageOptions;
import net.solarnetwork.nim.domain.SolarNodeImageReceipt;
import net.solarnetwork.nim.domain.SolarNodeImageResource;

/**
 * API for a node image creation service.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeImageService {

  /**
   * Authorize a user to create a image.
   * 
   * <p>
   * This method can be used to authorize a SolarUser user based on a pre-signed authorization
   * header for the {@code /solaruser/api/v1/sec/whoami} endpoint.
   * </p>
   * 
   * @param authorization
   *          a pre-signed header for the {@code /whoami} endpoint
   * @param authorizationDate
   *          the date for the authorization
   * @return a key to use for subsequent calls to other methods in this API
   */
  String authorize(String authorization, Date authorizationDate);

  /**
   * Create a new image using a base {@link SolarNodeImage} as a starting point, applying a set of
   * named resource and parameters.
   * 
   * <p>
   * The {@code key} value passed in should be securely random. A random UUID string is a good
   * choice.
   * </p>
   * 
   * @param key
   *          a unique ID to associate with the image task; this key will be needed to check on the
   *          status of the task later via the {@link #getReceipt()} method
   * @param sourceImage
   *          the base image that serves as the starting point of the customized image
   * @param resources
   *          a set of named resources to customize the image with
   * @param options
   *          options to use during the image customization process
   * @return a receipt for the task
   * @throws IOException
   *           if an IO error occurs
   */
  SolarNodeImageReceipt createImage(String key, SolarNodeImage sourceImage,
      Iterable<SolarNodeImageResource> resources, SolarNodeImageOptions options) throws IOException;

  /**
   * Get a receipt for a given key and ID.
   * 
   * @param key
   *          the same {@code key} value passed to
   *          {@link #createImage(String, SolarNodeImage, Map, Map)} previously
   * @param id
   *          the ID of the {@link SolarNodeImageReceipt} to get
   * @return the receipt, or {@literal null} if not available
   */
  SolarNodeImageReceipt getReceipt(String key, String id);

}
