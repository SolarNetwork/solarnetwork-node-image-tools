/* ==================================================================
 * SolarNodeImageResource.java - 19/10/2017 6:42:19 AM
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

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.InputStreamSource;

/**
 * A resource to apply to a node image.
 * 
 * @author matt
 * @version 1.0
 */
public interface SolarNodeImageResource extends InputStreamSource {

  /**
   * Return the filename of the resource.
   * 
   * <p>
   * This is expected to return the desired filename of the resource, which for a
   * {@code MultipartFile} would be the <i>original</i> filename.
   * </p>
   */
  String getFilename();

  /**
   * Transfer the resource to the given destination file, if possible.
   * 
   * <p>
   * This method is expected to abide by the same rules as outlined in
   * {@link org.springframework.web.multipart.MultipartFile#transferTo(File)}, namely this method
   * can only be called once per instance.
   * </p>
   * 
   * @param dest
   *          the destination to transfer the resource to
   * @throws IOException
   *           in case of reading or writing errors
   * @throws IllegalStateException
   *           if the file has already been moved in the filesystem and is not available anymore for
   *           another transfer
   */
  void transferTo(File dest) throws IOException, IllegalStateException;
}
