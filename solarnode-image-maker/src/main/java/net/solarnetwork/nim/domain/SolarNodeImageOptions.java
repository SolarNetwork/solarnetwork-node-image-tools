/* ==================================================================
 * SolarNodeImageOptions.java - 19/10/2017 3:08:41 PM
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

import java.util.Map;

/**
 * Options and parameters to use during the image creation process.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeImageOptions {

  private Map<String, String> environment;
  private Map<String, Object> parameters;
  private boolean verbose = false;

  /**
   * Get the environment variables.
   * 
   * @return the environment variables, or {@literal null}
   */
  public Map<String, String> getEnvironment() {
    return environment;
  }

  /**
   * Set a map of environment variables to pass the image customization task.
   * 
   * @param environment
   *          the environment variables to pass
   */
  public void setEnvironment(Map<String, String> environment) {
    this.environment = environment;
  }

  /**
   * Get parameters to pass to the {@link net.solarentwork.nim.service.NodeImageService}.
   * 
   * @return the parameters, or {@literal null}
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * Set the parameters to pass to the {@link net.solarentwork.nim.service.NodeImageService}.
   * 
   * <p>
   * The supported values are {@link net.solarentwork.nim.service.NodeImageService} dependent.
   * </p>
   * 
   * @param parameters
   *          the parameters to set
   */
  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  /**
   * Get a single parameter value.
   * 
   * @param key
   *          the key of the parameter value to get
   * @return the value, or {@literal null} if not available
   */
  public Object getParameterValue(String key) {
    return (this.parameters != null ? this.parameters.get(key) : null);
  }

  /**
   * Get the verbose flag.
   * 
   * @return {@literal true} for verbose output
   */
  public boolean isVerbose() {
    return verbose;
  }

  /**
   * Turn on verbose output and return as the message.
   * 
   * @param verbose
   *          enable verbose output; defaults to {@literal false}
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

}
