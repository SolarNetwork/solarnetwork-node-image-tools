/* ==================================================================
 * NodeImageScriptValidator.java - 21/10/2017 7:19:48 AM
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

import java.nio.file.Path;

import net.solarnetwork.nim.NodeImageScriptException;

/**
 * API for validating a customization script.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeImageScriptValidator {

  /**
   * Validate the contents of a script resource.
   * 
   * @param path
   *          the script file to validate
   * @throws NodeImageScriptException
   *           if an unsupported script command was detected
   */
  public void validate(Path path) throws NodeImageScriptException;

}
