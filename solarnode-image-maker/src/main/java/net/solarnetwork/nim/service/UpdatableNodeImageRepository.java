/* ==================================================================
 * UpdatableNodeImageRepository.java - 19/10/2017 10:39:05 AM
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

import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.util.TaskStepTracker;

/**
 * Extension of {@link NodeImageRepository} that allows updates.
 * 
 * @author matt
 * @version 1.0
 */
public interface UpdatableNodeImageRepository extends NodeImageRepository {

  /**
   * Saves a given node image.
   * 
   * <p>
   * This method must physically copy the image data into the repository.
   * </p>
   * 
   * @param image
   *          the image to save
   * @param tracker
   *          a tracker to update progress on, or {@literal null} if progress tracking not needed
   * @return the saved image
   */
  SolarNodeImage save(SolarNodeImage image, TaskStepTracker tracker);

  /**
   * Deletes a given node image.
   * 
   * @param id
   *          the ID of the image to delete
   */
  void delete(String id);

}
