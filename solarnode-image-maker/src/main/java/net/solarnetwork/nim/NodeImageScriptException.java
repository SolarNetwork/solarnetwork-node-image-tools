/* ==================================================================
 * NodeImageScriptException.java - 21/10/2017 6:59:13 AM
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

package net.solarnetwork.nim;

/**
 * An exception thrown when processing an image customization script.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeImageScriptException extends RuntimeException {

  private static final long serialVersionUID = -7376272144372482980L;

  private final String scriptName;
  private final int lineNumber;

  /**
   * Constructor.
   * 
   * @param scriptName
   *          the name of the script that had the problem
   * @param lineNumber
   *          the line number in the script where the problem occurred
   * @param message
   *          an error message
   */
  public NodeImageScriptException(String scriptName, int lineNumber, String message) {
    super(message);
    this.scriptName = scriptName;
    this.lineNumber = lineNumber;
  }

  /**
   * Get the name of the script that had the problem.
   * 
   * @return the script name
   */
  public String getScriptName() {
    return scriptName;
  }

  /**
   * Get the line number in the script where the problem occurred.
   * 
   * @return the line number
   */
  public int getLineNumber() {
    return lineNumber;
  }

}
