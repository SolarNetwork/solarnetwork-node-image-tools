/* ==================================================================
 * NodeImageServiceConfig.java - 19/10/2017 11:34:44 AM
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.service.impl.GuestfsNodeImageService;

/**
 * Configuration for the node image service.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class NodeImageServiceConfig {

  @Autowired
  @Qualifier("dest")
  private UpdatableNodeImageRepository destRepository;

  @Bean
  public GuestfsNodeImageService nodeImageService() {
    GuestfsNodeImageService nis = new GuestfsNodeImageService();
    nis.setNodeImageRepository(destRepository);
    return nis;
  }

}
