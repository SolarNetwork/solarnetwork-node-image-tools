/* ==================================================================
 * NodeImageRepository.java - 17/10/2017 5:34:54 PM
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

/**
 * API for SolarNode image resources.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeImageRepository {

  /**
   * Returns all available images.
   * 
   * @return all entities
   */
  Iterable<SolarNodeImage> findAll();

  /**
   * Retrieves a SolarNodeImage by its id.
   * 
   * @param id
   *          must not be {@literal null}.
   * @return the entity with the given id or {@literal null} if none found
   * @throws IllegalArgumentException
   *           if {@code id} is {@literal null}
   */
  SolarNodeImage findOne(String id);

}
