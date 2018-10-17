/* ==================================================================
 * SolarNodeImageInfoSortById.java - 18/10/2018 7:15:37 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

import java.util.Comparator;

/**
 * Comparator for {@link SolarNodeImageInfo} that sorts by ID in a case-insensitive manner.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeImageInfoSortById implements Comparator<SolarNodeImageInfo> {

  /** A default instance that can be used for sorting. */
  public static final SolarNodeImageInfoSortById SORT_BY_ID = new SolarNodeImageInfoSortById();

  @Override
  public int compare(SolarNodeImageInfo o1, SolarNodeImageInfo o2) {
    String l = (o1 != null ? o1.getId() : null);
    String r = (o2 != null ? o2.getId() : null);
    if (l == null && r == null) {
      return 0;
    } else if (l == null) {
      return -1;
    } else if (r == null) {
      return 1;
    }
    return l.compareToIgnoreCase(r);
  }

}
