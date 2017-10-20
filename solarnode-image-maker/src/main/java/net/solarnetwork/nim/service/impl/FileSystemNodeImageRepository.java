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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;
import net.solarnetwork.nim.domain.ResourceSolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.util.DecompressingResource;
import net.solarnetwork.nim.util.MaxCompressorStreamFactory;
import net.solarnetwork.nim.util.MessageDigestOutputStream;
import net.solarnetwork.nim.util.TaskStepTracker;
import net.solarnetwork.nim.util.TaskStepTrackerOutputStream;

/**
 * {@link NodeImageRepository} implementation that uses a file system hierarchy to store node images
 * as files, along with JSON metadata files that describe the image files.
 * 
 * @author matt
 * @version 1.0
 */
public class FileSystemNodeImageRepository extends AbstractNodeImageRepository
    implements UpdatableNodeImageRepository {

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
    try {
      ResourceSolarNodeImage result = findOneInternal(id);
      if (result != null) {
        result = new ResourceSolarNodeImage(result.getInfo(),
            new DecompressingResource(result.getImageResource()));
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Error listing node image infos: " + e.getMessage(), e);
    }
  }

  @Override
  public SolarNodeImage findOneCompressed(String id) {
    try {
      return findOneInternal(id);
    } catch (IOException e) {
      throw new RuntimeException("Error listing node image infos: " + e.getMessage(), e);
    }
  }

  @Override
  public void delete(String id) {
    List<Path> files;
    try {
      files = imageFiles(id);
    } catch (IOException e) {
      throw new RuntimeException("Error finding image files for ID " + id, e);

    }
    for (Path path : files) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        throw new RuntimeException("Error deleting image file " + path, e);
      }
    }
  }

  private List<Path> imageFiles(String id) throws IOException {
    // get paths to the metadata AND any associated image (for which we don't know the extension)
    final String imagePathsPrefix = id + ".";
    return Files.walk(rootDirectory)
        .filter(p -> p.getFileName().toString().startsWith(imagePathsPrefix))
        .collect(Collectors.toList());
  }

  private ResourceSolarNodeImage findOneInternal(String id) throws IOException {
    final String imageInfoPath = id + ".json";

    // get paths to the metadata AND any associated image (for which we don't know the extension)
    List<Path> imagePaths = imageFiles(id);
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
    return new ResourceSolarNodeImage(info, rsrc);
  }

  @Override
  public SolarNodeImage save(SolarNodeImage image, TaskStepTracker tracker) {
    String id = image.getId();
    String filename = image.getFilename();
    Path file = rootDirectory.resolve(MaxCompressorStreamFactory.getCompressedFilename(
        MaxCompressorStreamFactory.XZ, id + "." + StringUtils.getFilenameExtension(filename)));

    // compute the digests of both the input and output streams while copying...
    final long expectedInputContentLength = image.getUncompressedContentLength();
    MessageDigest inputDigest = DigestUtils.getSha256Digest();
    MessageDigest outputDigest = DigestUtils.getSha256Digest();

    MutableLong inputContentLength = new MutableLong(0);
    MutableLong outputContentLength = new MutableLong(0);

    try (InputStream in = image.getInputStream();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
      log.info("Compressing image {} to {} using {} @ {}%", image.getFilename(), file,
          getCompressionType(), (int) (getCompressionRatio() * 100));
      FileCopyUtils.copy(in,
          new TaskStepTrackerOutputStream(expectedInputContentLength, tracker,
              new MessageDigestOutputStream(inputDigest, inputContentLength,
                  createCompressorOutputStream(
                      new MessageDigestOutputStream(outputDigest, outputContentLength, out)))));

      String jsonFilename = id + ".json";
      Path jsonFile = rootDirectory.resolve(jsonFilename);
      SolarNodeImageInfo info = new BasicSolarNodeImageInfo(id,
          new String(Hex.encodeHex(outputDigest.digest())), outputContentLength.longValue(),
          new String(Hex.encodeHex(inputDigest.digest())), inputContentLength.longValue());
      try {
        OBJECT_MAPPER.writeValue(jsonFile.toFile(), info);
      } catch (IOException e) {
        throw new RuntimeException("Error writing image metadata to " + jsonFile, e);
      }
      FileSystemResource rsrc = new FileSystemResource(file.toFile());
      return new ResourceSolarNodeImage(info, rsrc);
    } catch (CompressorException | IOException e) {
      throw new RuntimeException("Error writing image data to " + file, e);
    }
  }

}
