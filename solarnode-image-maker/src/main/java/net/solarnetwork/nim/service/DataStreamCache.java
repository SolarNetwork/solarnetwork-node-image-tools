/* ==================================================================
 * DataStreamCache.java - 22/10/2017 12:48:42 PM
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
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * API for a cached data stream.
 * 
 * <p>
 * This API is used to provide locally cached copies of remote resources.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface DataStreamCache {

  /**
   * Get a data stream.
   * 
   * <p>
   * This returns a cached stream if possible. If a cached stream is not available, the given
   * {@code supplier} will be asked to provide the stream, and the result will be cached for future
   * use.
   * </p>
   * 
   * @param key
   *          a unique key for the stream
   * @param supplier
   *          a data stream provider, in case the stream is not found in the cache
   * @return the data stream
   * @throws IOException
   *           if any IO error occurs
   */
  InputStream get(String key, Supplier<InputStream> supplier) throws IOException;

}
