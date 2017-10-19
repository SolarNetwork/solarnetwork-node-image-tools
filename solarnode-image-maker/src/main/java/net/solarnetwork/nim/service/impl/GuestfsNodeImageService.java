/* ==================================================================
 * GuestfsNodeImageService.java - 19/10/2017 6:39:35 AM
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.NodeImageService;

/**
 * {@link NodeImageService} using libguestfs.
 * 
 * @author matt
 * @version 1.0
 */
public class GuestfsNodeImageService extends AbstractNodeImageService {

  /**
   * The name of the resource that holds the {@literal guestfish} commands to execute.
   */
  public static final String SCRIPT_RESOURCE_NAME_EXTENSION = ".fish";

  private String guestfishBin = "guestfish";

  @Override
  protected ImageSetupResult createImageInternal(String key, SolarNodeImageInfo imageInfo,
      Path imageFile, List<Path> resources, Map<String, ?> parameters) throws IOException {
    Path workingDir = imageFile.getParent();
    ProcessBuilder pb = setupProcess(workingDir, imageFile, resources, parameters);
    Process proc = pb.start();
    StringBuilder output = new StringBuilder();
    try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
      String line = null;
      while ((line = in.readLine()) != null) {
        if (output.length() > 0) {
          output.append('\n');
        }
        output.append(line);
      }
    }
    try {
      proc.waitFor();
    } catch (InterruptedException e) {
      log.warn("Interrupted waiting for guestfish command to complete");
    }
    if (proc.exitValue() != 0) {
      log.error("guestfish command returned non-zero exit code {}: {}", proc.exitValue(), output);
      throw new IOException(
          "guestfish command returned non-zero exit code " + proc.exitValue() + ": " + output);
    }
    return new ImageSetupResult(imageFile, output.toString(), true);
  }

  private ProcessBuilder setupProcess(Path workingDir, Path imageFile, List<Path> resources,
      Map<String, ?> parameters) {
    List<String> cmd = new ArrayList<>(8);
    cmd.add(guestfishBin);
    cmd.add("--rw"); // mount image read+write
    cmd.add(imageFile.getFileName().toString()); // assumed to be in working dir
    cmd.add("--inspector"); // auto-mount filesystems of image

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(workingDir.toFile());

    Map<String, String> env = (parameters != null
        ? parameters.entrySet().stream().filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()))
        : Collections.emptyMap());
    pb.environment().putAll(env);

    Path scriptFile = resources.stream()
        .filter(p -> p.getFileName().toString().endsWith(SCRIPT_RESOURCE_NAME_EXTENSION))
        .findFirst().orElse(null);
    if (scriptFile == null) {
      throw new IllegalArgumentException(
          "No " + SCRIPT_RESOURCE_NAME_EXTENSION + " resource provided");
    }
    log.info("Executing command {} <{}", cmd, scriptFile);
    pb.redirectInput(scriptFile.toFile());

    pb.command(cmd);

    return pb;
  }

  /**
   * Set the {@literal guestfish} command to use.
   * 
   * <p>
   * If {@literal guestfish} is not available in the default process path, this should be configured
   * as the full path to the executable, for example {@literal /usr/local/bin/guestfish}.
   * </p>
   * 
   * @param guestfishBin
   *          the {@literal guestfish} command to use; defaults to {@literal guestfish}
   */
  public void setGuestfishBin(String guestfishBin) {
    this.guestfishBin = guestfishBin;
  }

}
