/* ==================================================================
 * FileSystemNodeImageRepository.java - 17/10/2017 5:43:08 PM
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;
import net.solarnetwork.nim.domain.ResourceSolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.util.DecompressingResource;

/**
 * {@link NodeImageRepository} implementation that uses a file system hierarchy to store node images
 * as files, along with JSON metadata files that describe the image files.
 * 
 * @author matt
 * @version 1.0
 */
public class FileSystemNodeImageRepository implements NodeImageRepository {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

  private final Path rootDirectory;

  public FileSystemNodeImageRepository(Path rootDirectory) {
    super();
    this.rootDirectory = rootDirectory;
  }

  /**
   * Parse a {@link BasicSolarNodeImageInfo} from a JSON file.
   * 
   * @param jsonFile
   *          the path to the JSON file to parse
   * @return the parsed instance
   */
  public static BasicSolarNodeImageInfo infoFromJsonFile(Path jsonFile) {
    try {
      return OBJECT_MAPPER.readValue(jsonFile.toFile(), BasicSolarNodeImageInfo.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Stream<Path> infoFileStream() throws IOException {
    return Files.walk(rootDirectory).filter(p -> p.getFileName().toString().endsWith(".json"));
  }

  @Override
  public Iterable<SolarNodeImageInfo> findAll() {
    try {
      return infoFileStream().map(FileSystemNodeImageRepository::infoFromJsonFile)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("Error listing node image infos: " + e.getMessage(), e);
    }
  }

  @Override
  public SolarNodeImage findOne(String id) {
    final String imagePathsPrefix = id + ".";
    final String imageInfoPath = id + ".json";
    try {
      // get paths to the metadata AND any associated image (for which we don't know the extension)
      List<Path> imagePaths = Files.walk(rootDirectory)
          .filter(p -> p.getFileName().toString().startsWith(imagePathsPrefix))
          .collect(Collectors.toList());
      Path infoPath = imagePaths.stream()
          .filter(p -> p.getFileName().toString().equals(imageInfoPath)).findFirst().orElse(null);
      if (infoPath == null) {
        return null;
      }
      Path imagePath = imagePaths.stream()
          .filter(p -> !p.getFileName().toString().equals(imageInfoPath)).findFirst().orElse(null);
      if (imagePath == null) {
        return null;
      }
      SolarNodeImageInfo info = infoFromJsonFile(infoPath);
      FileSystemResource rsrc = new FileSystemResource(imagePath.toFile());
      return new ResourceSolarNodeImage(info, new DecompressingResource(rsrc));
    } catch (IOException e) {
      throw new RuntimeException("Error listing node image infos: " + e.getMessage(), e);
    }
  }

}
