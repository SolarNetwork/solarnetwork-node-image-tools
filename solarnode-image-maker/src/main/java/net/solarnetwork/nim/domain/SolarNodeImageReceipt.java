/* ==================================================================
 * SolarNodeImageReceipt.java - 18/10/2017 7:23:25 PM
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

import java.util.concurrent.Future;

/**
 * A receipt and status information about an asynchronous image task.
 * 
 * <p>
 * The {@link #get()} method will return when the custom node image work is complete.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface SolarNodeImageReceipt extends Future<SolarNodeImage> {

  /**
   * Get a unique ID for this work.
   * 
   * @return the unique ID
   */
  String getId();

  /**
   * Flag indicating if the task has started.
   * 
   * @return {@literal true} if the task has started, or {@literal false} if it is still queued to
   *         start later
   */
  boolean isStarted();

  /**
   * Get a status message.
   * 
   * @return a status message
   */
  String getMessage();

  /**
   * Get the amount of work that has been completed, as a fractional percentage between {@literal 0}
   * and {@literal 1}.
   * 
   * @return the amount of work completed
   */
  double getPercentComplete();

  /**
   * Get information about the final image, once ready.
   * 
   * @return the finished image info, or {@literal null} if not available yet
   */
  SolarNodeImageInfo getImageInfo();
}
