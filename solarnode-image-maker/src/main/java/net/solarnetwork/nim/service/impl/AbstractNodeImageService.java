/* ==================================================================
 * AbstractNodeImageService.java - 19/10/2017 7:05:43 AM
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;
import net.solarnetwork.nim.domain.ResourceSolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.domain.SolarNodeImageOptions;
import net.solarnetwork.nim.domain.SolarNodeImageReceipt;
import net.solarnetwork.nim.domain.SolarNodeImageResource;
import net.solarnetwork.nim.service.NodeImageService;
import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.util.SolarNodeImageReceiptFuture;
import net.solarnetwork.nim.util.TaskStepTracker;
import net.solarnetwork.nim.util.TaskStepTrackerOutputStream;

/**
 * Abstract base class for {@link NodeImageService} with basic common features.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractNodeImageService implements NodeImageService {

  private int receiptMaxAgeSeconds = (int) TimeUnit.HOURS.toSeconds(12);
  private Path stagingDir = Paths.get(System.getProperty("java.io.tmpdir"));
  private ExecutorService executorService = Executors.newSingleThreadExecutor();
  private UpdatableNodeImageRepository nodeImageRepository;

  // @formatter:off
  private final ConcurrentMap<String, SolarNodeImageReceiptFuture> receipts 
      = new ConcurrentHashMap<>(8);
  // @formatter:on

  /** A class-level logger. */
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * An internal result object for the image setup process.
   */
  public static final class ImageSetupResult {

    private final Path imageFile;
    private final String message;
    private final boolean success;

    /**
     * Constructor.
     * 
     * @param imageFile
     *          the path to the final image file, or {@literal null} if an error occurred
     * @param message
     *          a result message (success or failure)
     * @param success
     *          {@literal true} if the image was setup successfully
     */
    public ImageSetupResult(Path imageFile, String message, boolean success) {
      super();
      this.imageFile = imageFile;
      this.message = message;
      this.success = success;
    }

    public Path getImageFile() {
      return imageFile;
    }

    public String getMessage() {
      return message;
    }

    public boolean isSuccess() {
      return success;
    }

  }

  @Override
  public SolarNodeImageReceipt createImage(String key, SolarNodeImage sourceImage,
      Iterable<SolarNodeImageResource> resources, SolarNodeImageOptions options)
      throws IOException {
    final String receiptId = UUID.randomUUID().toString();
    final String taskId = taskId(receiptId, key);
    final Path root = Files.createTempDirectory(stagingDir, "node-image-");
    final Path imageDest = root.resolve(sourceImage.getId() + ".img");

    // copy input data on calling thread, so things like multipart temp files aren't deleted
    final List<Path> resourceFiles = new ArrayList<>(8);
    for (SolarNodeImageResource rsrc : resources) {
      Path dest = root.resolve(rsrc.getFilename());
      resourceFiles.add(dest);
      log.info("Transferring resource {} to {}", rsrc.getFilename(), dest);
      rsrc.transferTo(dest.toFile());
    }

    // steps are: 1) uncompress image 2) customize image 3) compress image
    final TaskStepTracker tracker = new TaskStepTracker(3);

    Callable<SolarNodeImage> task = new Callable<SolarNodeImage>() {

      @Override
      public SolarNodeImage call() throws Exception {
        tracker.start();
        tracker.setMessage("Uncompressing source image");
        log.info("Transferring image {} to {}", sourceImage.getId(), imageDest);
        try {
          // TODO: use DigestOutputStream to verify digest of written file
          FileCopyUtils.copy(sourceImage.getInputStream(),
              new TaskStepTrackerOutputStream(sourceImage.getUncompressedContentLength(), tracker,
                  new BufferedOutputStream(new FileOutputStream(imageDest.toFile()))));
          tracker.completeStep(); // 1

          tracker.setMessage("Customizing image");
          ImageSetupResult result = createImageInternal(key, sourceImage, imageDest, resourceFiles,
              options, tracker);
          tracker.completeStep(); // 2

          if (result.isSuccess() && result.getImageFile() != null) {
            // compress the image while copying into repo
            tracker.setMessage("Compressing image");
            Resource imageFileResource = new FileSystemResource(result.getImageFile().toFile());
            ResourceSolarNodeImage image = new ResourceSolarNodeImage(new BasicSolarNodeImageInfo(
                taskId, null, 0, null, imageFileResource.contentLength()), imageFileResource);
            SolarNodeImage output = nodeImageRepository.save(image, tracker);
            tracker.completeStep(); // 3
            tracker.setMessage("Done");
            return output;
          }
          throw new RuntimeException("Image " + key + " setup failed: " + result.getMessage());
        } catch (Exception e) {
          tracker.setMessage(e.getMessage());
          throw e;
        } finally {
          // clean up
          log.info("Deleting staging dir {}", root);
          FileSystemUtils.deleteRecursively(root.toFile());
        }
      }
    };

    Future<SolarNodeImage> result = executorService.submit(task);
    SolarNodeImageReceiptFuture receipt = new SolarNodeImageReceiptFuture(receiptId, result,
        tracker);
    receipts.put(taskId, receipt);
    return receipt;
  }

  @Override
  public SolarNodeImageReceipt getReceipt(String key, String id) {
    String taskId = taskId(id, key);
    return receipts.get(taskId);
  }

  /**
   * Remove expired receipts.
   * 
   * <p>
   * This method is designed to be called periodically to remove expired receipts.
   * </p>
   * 
   * @return the number of receipts removed
   */
  public int cleanExpiredReceipts() {
    int removed = 0;
    for (Iterator<SolarNodeImageReceiptFuture> itr = receipts.values().iterator(); itr.hasNext();) {
      SolarNodeImageReceiptFuture task = itr.next();
      // TODO: maybe only remove done tasks, in case there is a backlog?
      if ((task.getCreated() + (receiptMaxAgeSeconds * 1000)) < System.currentTimeMillis()) {
        if (!task.isDone()) {
          task.cancel(true);
        }
        itr.remove();
        removed++;
      }
    }
    return removed;
  }

  private String taskId(String receiptId, String key) {
    return DigestUtils.sha256Hex(receiptId + key);
  }

  /**
   * Create the final node image.
   * 
   * <p>
   * This method will be called from a task submitted to the configured {@code executorService}. The
   * uncompressed base image and all resources will have been copied to a staging directory.
   * </p>
   * 
   * @param key
   *          the key originally passed to
   *          {@link #createImage(String, SolarNodeImage, Iterable, Map)}
   * @param imageInfo
   *          the base image info
   * @param imageFile
   *          the uncompressed image file
   * @param resources
   *          any resources to apply to the image
   * @param options
   *          options to use when customizing the image
   * @param tracker
   *          a step tracker
   * @return the result object
   */
  protected abstract ImageSetupResult createImageInternal(String key, SolarNodeImageInfo imageInfo,
      Path imageFile, List<Path> resources, SolarNodeImageOptions options, TaskStepTracker tracker)
      throws IOException;

  /**
   * Set the "staging" directory where all work is performed.
   * 
   * <p>
   * All image resources will be copied into this directory, so the filesystem it resides on should
   * be large enough to hold all the resources and the image file itself.
   * </p>
   * 
   * @param stagingDir
   *          the staging directory to use; defaults to the {@code java.io.tmpdir} system property
   */
  public void setStagingDir(Path stagingDir) {
    this.stagingDir = stagingDir;
  }

  /**
   * Set a executor service to handle work tasks with.
   * 
   * @param executorService
   *          the service to set; defaults to a single thread implementation
   */
  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  /**
   * Set the maximum age, in seconds, for receipts to be maintained before removing them.
   * 
   * @param receiptMaxAgeSeconds
   *          the maximum age to retain receipts; defaults to 12 hours
   */
  public void setReceiptMaxAgeSeconds(int receiptMaxAgeSeconds) {
    this.receiptMaxAgeSeconds = receiptMaxAgeSeconds;
  }

  /**
   * Set the node image repo to use for storing the result images.
   * 
   * @param nodeImageRepository
   *          the repo to use
   */
  public void setNodeImageRepository(UpdatableNodeImageRepository nodeImageRepository) {
    this.nodeImageRepository = nodeImageRepository;
  }

}
