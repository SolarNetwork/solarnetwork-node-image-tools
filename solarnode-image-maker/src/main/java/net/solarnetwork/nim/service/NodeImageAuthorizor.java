/* ==================================================================
 * NodeImageAuthorizor.java - 30/10/2017 3:40:42 PM
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

import java.util.Date;
import java.util.Map;

/**
 * API for SolarNet access.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeImageAuthorizor {

  /**
   * Validate some authorization value.
   * 
   * @param authorization
   *          the authorization value to validate
   * @param authorizationDate
   *          a date associated with the authorization
   * @return resulting data; implementation specific
   * @throws RuntimeException
   *           if any error occurs
   */
  Map<String, ?> authorize(String authorization, Date authorizationDate);

}
