/* ==================================================================
 * NodeImageRepositoryConfig.java - 18/10/2017 7:06:36 AM
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

import java.io.File;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import net.solarnetwork.nim.service.impl.FileSystemNodeImageRepository;

/**
 * Configuration for the node image repository.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class NodeImageRepositoryConfig {

  @Value("${repo.source.fs.path:var/repo}")
  private File fsSourceRepoRootDirectory = new File("var/repo");

  @Value("${repo.dest.fs.path:/var/tmp/node-image-repo}")
  private File fsDestRepoRootDirectory = new File("/var/tmp/node-image-repo");

  @Bean
  @Profile({ "default", "development" })
  @Qualifier("source")
  public FileSystemNodeImageRepository fsSourceNodeImageRepository() {
    if (!fsSourceRepoRootDirectory.isDirectory()) {
      if (!fsSourceRepoRootDirectory.mkdirs()) {
        throw new RuntimeException("FS src repo root " + fsSourceRepoRootDirectory.getAbsolutePath()
            + " does not exist and unable to create");
      }
    }
    return new FileSystemNodeImageRepository(fsSourceRepoRootDirectory.toPath());
  }

  @Bean
  @Qualifier("dest")
  public FileSystemNodeImageRepository fsDestNodeImageRepository() {
    if (!fsDestRepoRootDirectory.isDirectory()) {
      if (!fsDestRepoRootDirectory.mkdirs()) {
        throw new RuntimeException("FS dest repo root " + fsDestRepoRootDirectory.getAbsolutePath()
            + " does not exist and unable to create");
      }
    }
    return new FileSystemNodeImageRepository(fsDestRepoRootDirectory.toPath());
  }

}
