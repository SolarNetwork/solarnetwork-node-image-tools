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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import net.solarnetwork.nim.AuthorizationException;
import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;
import net.solarnetwork.nim.domain.ResourceSolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.domain.SolarNodeImageOptions;
import net.solarnetwork.nim.domain.SolarNodeImageReceipt;
import net.solarnetwork.nim.domain.SolarNodeImageResource;
import net.solarnetwork.nim.service.NodeImageAuthorizor;
import net.solarnetwork.nim.service.NodeImageService;
import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.util.DecompressingSolarNodeImage;
import net.solarnetwork.nim.util.MessageDigestInputStream;
import net.solarnetwork.nim.util.SolarNodeImageReceiptFuture;
import net.solarnetwork.nim.util.TaskStepTracker;
import net.solarnetwork.nim.util.TaskStepTrackerOutputStream;
import net.solarnetwork.util.CachedResult;

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
  private NodeImageAuthorizor nodeImageAuthorizor = null;

  // @formatter:off
  private final ConcurrentMap<String, SolarNodeImageReceiptFuture> receipts 
      = new ConcurrentHashMap<>(8);
  private final ConcurrentMap<String, CachedResult<String>> authorizedKeys
      = new  ConcurrentHashMap<>();
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
  public int activeSessionCount() {
    return authorizedKeys.keySet().size();
  }

  @Override
  public String authorize(String authorization, Date authorizationDate) {
    String key = DigestUtils.sha256Hex(UUID.randomUUID().toString());
    if (nodeImageAuthorizor == null) {
      return key;
    }
    nodeImageAuthorizor.authorize(authorization, authorizationDate);
    authorizedKeys.put(key, new CachedResult<String>(key, receiptMaxAgeSeconds, TimeUnit.SECONDS));
    return key;
  }

  @Override
  public SolarNodeImageReceipt createImage(String key, SolarNodeImage sourceImage,
      Iterable<SolarNodeImageResource> resources, SolarNodeImageOptions options)
      throws IOException {
    // validate key is authorized, as long as an Authorizor is configured
    if (key == null || (nodeImageAuthorizor != null && !authorizedKeys.containsKey(key))) {
      throw new AuthorizationException("Key is not authorized");
    }
    if (sourceImage == null) {
      throw new IllegalArgumentException("No source image provided.");
    }

    final String receiptId = UUID.randomUUID().toString();
    final String taskId = taskId(receiptId, key);
    final Path root = Files.createTempDirectory(stagingDir, "node-image-");
    final Path imageDest = root.resolve(sourceImage.getId() + ".img");
    final String outputId = UUID.randomUUID().toString();

    // copy input data on calling thread, so things like multipart temp files aren't deleted
    final List<Path> resourceFiles = new ArrayList<>(8);
    for (SolarNodeImageResource rsrc : resources) {
      if (rsrc.getFilename() == null || rsrc.getFilename().isEmpty()) {
        continue;
      }
      Path dest = root.resolve(rsrc.getFilename());
      resourceFiles.add(dest);
      log.info("Transferring resource {} to {}", rsrc.getFilename(), dest);
      rsrc.transferTo(dest.toFile());
    }

    // steps are: 1) uncompress image 2) customize image 3) compress image
    final TaskStepTracker tracker = new TaskStepTracker(
        2 + nodeImageRepository.getSaveTaskStepCount());
    final MutableLong uncompressedLength = new MutableLong(0);
    final MessageDigest uncompressedDigest = DigestUtils.getSha256Digest();

    Callable<SolarNodeImage> task = new Callable<SolarNodeImage>() {

      @Override
      public SolarNodeImage call() throws Exception {
        tracker.start();
        tracker.setMessage("Uncompressing source image");
        log.info("Decompressing image {} to {}", sourceImage.getId(), imageDest);
        try {
          FileCopyUtils.copy(
              new MessageDigestInputStream(uncompressedDigest, uncompressedLength,
                  new DecompressingSolarNodeImage(sourceImage).getInputStream()),
              new TaskStepTrackerOutputStream(sourceImage.getUncompressedContentLength(), tracker,
                  new BufferedOutputStream(new FileOutputStream(imageDest.toFile()))));
          tracker.completeStep(); // step 1

          // verify digest
          final String decompressedImageDigest = new String(
              Hex.encodeHex(uncompressedDigest.digest()));
          if (!sourceImage.getUncompressedSha256().equalsIgnoreCase(decompressedImageDigest)) {
            throw new RuntimeException("Image " + sourceImage.getId()
                + " uncompressed SHA-256 digest " + decompressedImageDigest + " does not match "
                + sourceImage.getUncompressedSha256());
          }

          tracker.setMessage("Customizing image");
          ImageSetupResult result = createImageInternal(key, sourceImage, imageDest, resourceFiles,
              options, tracker); // step 2
          tracker.completeStep();

          if (result.isSuccess() && result.getImageFile() != null) {
            // compress the image while copying into repo
            tracker.setMessage("Compressing customized image");
            Resource imageResource = new FileSystemResource(result.getImageFile().toFile());
            ResourceSolarNodeImage image = new ResourceSolarNodeImage(
                new BasicSolarNodeImageInfo(outputId, null, 0, null, imageResource.contentLength()),
                imageResource);
            SolarNodeImage output = nodeImageRepository.save(image, tracker); // steps 3-N
            tracker.setMessage("Done");
            return output;
          }
          throw new RuntimeException("Image " + key + " setup failed: " + result.getMessage());
        } catch (Exception | Error e) {
          tracker.setMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
          try {
            nodeImageRepository.delete(outputId);
          } catch (Throwable t) {
            // ignore
            log.warn("Error cleaning up image {} after {}", outputId, e.getClass().getSimpleName());
          }
          throw e;
        } finally {
          // clean up
          log.info("Deleting staging dir {}", root);
          FileSystemUtils.deleteRecursively(root.toFile());
          tracker.complete();
          ReadableDuration dur = new Duration(
              TimeUnit.MILLISECONDS.toSeconds(tracker.getStartedDate()) * 1000,
              TimeUnit.MILLISECONDS.toSeconds(tracker.getCompletedDate()) * 1000);
          log.info("Task {} completed {} in {}", taskId,
              ("Done".equals(tracker.getMessage()) ? "successfully" : "with error"),
              PeriodFormat.wordBased().print(dur.toPeriod()));
        }
      }
    };

    Future<SolarNodeImage> result = executorService.submit(task);
    SolarNodeImageReceiptFuture receipt = new SolarNodeImageReceiptFuture(receiptId,
        sourceImage.getId(), result, tracker) {

      @Override
      protected String getDownloadUrlInternal(SolarNodeImage image) {
        return nodeImageRepository.getDownloadUrl(image);
      }

    };
    receipts.put(taskId, receipt);
    return receipt;
  }

  @Override
  public SolarNodeImageReceipt getReceipt(String key, String id) {
    // validate key is authorized, as long as an Authorizor is configured
    if (nodeImageAuthorizor != null && !authorizedKeys.containsKey(key)) {
      throw new AuthorizationException("Key is not authorized");
    }
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
      if ((task.getCreatedDate() + (receiptMaxAgeSeconds * 1000)) < System.currentTimeMillis()) {
        if (!task.isDone()) {
          task.cancel(true);
        }
        SolarNodeImageInfo info = task.getImageInfo();
        if (info != null) {
          nodeImageRepository.delete(info.getId());
        }
        itr.remove();
        removed++;
      }
    }

    // also remove expired keys
    for (Iterator<CachedResult<String>> itr = authorizedKeys.values().iterator(); itr.hasNext();) {
      CachedResult<String> key = itr.next();
      if (!key.isValid()) {
        itr.remove();
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
   *          a step tracker; the implementing method must call
   *          {@link TaskStepTracker#completeStep()} at many times as
   *          {@link UpdatableNodeImageRepository#getSaveTaskStepCount()} returns
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

  /**
   * Set the NodeImageAuthorizor to use.
   * 
   * @param nodeImageAuthorizor
   *          the service to use; if not configured, authorization will not be required
   */
  public void setNodeImageAuthorizor(NodeImageAuthorizor nodeImageAuthorizor) {
    this.nodeImageAuthorizor = nodeImageAuthorizor;
  }

}
