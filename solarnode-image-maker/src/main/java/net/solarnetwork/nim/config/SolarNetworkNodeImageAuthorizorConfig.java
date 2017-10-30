/* ==================================================================
 * SolarNetworkNodeImageAuthorizorConfig.java - 30/10/2017 3:53:26 PM
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

package net.solarnetwork.nim.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import net.solarnetwork.nim.service.impl.SolarNetworkNodeImageAuthorizor;

/**
 * SolarNetwork client configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("snauth")
public class SolarNetworkNodeImageAuthorizorConfig {

  @Value("${solarnet.baseUrl:https://data.solarnetwork.net}")
  private String solarNetBaseUrl = "https://data.solarnetwork.net";

  /**
   * Get the NodeImageAuthorizor bean.
   * 
   * @return the client
   */
  @Bean
  public SolarNetworkNodeImageAuthorizor solarNetworkNodeImageAuthorizor() {
    SolarNetworkNodeImageAuthorizor client = new SolarNetworkNodeImageAuthorizor();
    client.setApiBaseUrl(solarNetBaseUrl);
    return client;
  }

}
