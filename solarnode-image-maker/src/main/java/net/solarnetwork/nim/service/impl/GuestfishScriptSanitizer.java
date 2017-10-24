/* ==================================================================
 * GuestfishScriptSanitizer.java - 21/10/2017 6:47:54 AM
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

package net.solarnetwork.nim.service.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import net.solarnetwork.nim.NodeImageScriptException;
import net.solarnetwork.nim.service.NodeImageScriptValidator;

/**
 * Helper that enforces restrictions on a {@literal guestfish} script.
 * 
 * @author matt
 * @version 1.0
 */
public class GuestfishScriptSanitizer implements NodeImageScriptValidator {

  /**
   * The pattern for a {@literal guestfish} local command.
   */
  public static final Pattern LOCAL_CMD_PAT = Pattern.compile("^\\s*<?!");

  /**
   * The pattern for a {@literal guestfish} local change directory command.
   */
  public static final Pattern LCD_CMD_PAT = Pattern.compile("^\\s*lcd");

  /**
   * The pattern for a {@literal guestfish} pipe command.
   */
  public static final Pattern LOCAL_PIPE_CMD_PAT = Pattern.compile("\\|");

  /**
   * Validate the contents of the script file.
   * 
   * @throws NodeImageScriptException
   *           if an unsupported script command was detected
   * @throws IOException
   *           if an error parsing the script occurs
   */
  @Override
  public void validate(Path scriptFile) throws NodeImageScriptException {
    try (BufferedReader in = new BufferedReader(new FileReader(scriptFile.toFile()))) {
      int lineno = 0;
      String line = null;
      while ((line = in.readLine()) != null) {
        lineno++;
        if (LOCAL_CMD_PAT.matcher(line).find()) {
          throw new NodeImageScriptException(scriptFile.getFileName().toString(), lineno,
              "Local commands are not supported: " + line);
        }
        if (LOCAL_PIPE_CMD_PAT.matcher(line).find()) {
          throw new NodeImageScriptException(scriptFile.getFileName().toString(), lineno,
              "Local pipe commands are not supported: " + line);

        }
        if (LCD_CMD_PAT.matcher(line).find()) {
          throw new NodeImageScriptException(scriptFile.getFileName().toString(), lineno,
              "The lcd command is not supported: " + line);
        }
      }
    } catch (IOException e) {
      throw new NodeImageScriptException(scriptFile.getFileName().toString(), -1,
          "Error processing script: " + e.getMessage());
    }
  }

}
