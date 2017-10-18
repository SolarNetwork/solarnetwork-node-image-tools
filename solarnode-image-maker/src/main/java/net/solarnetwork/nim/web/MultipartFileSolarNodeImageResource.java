/* ==================================================================
 * MultipartFileSolarNodeImageResource.java - 19/10/2017 6:50:23 AM
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

package net.solarnetwork.nim.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import net.solarnetwork.nim.domain.SolarNodeImageResource;

/**
 * {@link SolarNodeImageResource} for a {@link MultipartFile}.
 * 
 * @author matt
 * @version 1.0
 */
public class MultipartFileSolarNodeImageResource implements SolarNodeImageResource {

  private final MultipartFile part;

  /**
   * Constructor.
   * 
   * @param part
   *          the part to use
   */
  public MultipartFileSolarNodeImageResource(MultipartFile part) {
    super();
    this.part = part;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return part.getInputStream();
  }

  @Override
  public String getFilename() {
    return part.getOriginalFilename();
  }

  @Override
  public void transferTo(File dest) throws IOException, IllegalStateException {
    part.transferTo(dest);
  }

}
