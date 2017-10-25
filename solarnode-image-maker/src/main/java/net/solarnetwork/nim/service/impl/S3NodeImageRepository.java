/* ==================================================================
 * S3NodeImageRepository.java - 21/10/2017 11:51:01 AM
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.DataStreamCache;
import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.util.MaxCompressorStreamFactory;
import net.solarnetwork.nim.util.MessageDigestOutputStream;
import net.solarnetwork.nim.util.TaskStepTracker;
import net.solarnetwork.nim.util.TaskStepTrackerInputStream;
import net.solarnetwork.nim.util.TaskStepTrackerOutputStream;

/**
 * {@link NodeImageRepository} backed by Amazon S3 storage.
 * 
 * @author matt
 * @version 1.0
 */
public class S3NodeImageRepository extends AbstractNodeImageRepository
    implements UpdatableNodeImageRepository {

  /**
   * The S3 object key prefix to use for all metadata objects.
   */
  public static final String META_OBJECT_KEY_PREFIX = "node-image-meta/";

  /**
   * The S3 object key prefix to use for all data objects.
   */
  public static final String DATA_OBJECT_KEY_PREFIX = "node-image-data/";

  private static final String METADATA_OBJECT_KEY_SUFFIX = ".json";
  private static final String IMAGE_OBJECT_KEY_SUFFIX = ".img";

  private final AmazonS3 client;
  private final String bucketName;
  private final String objectKeyPrefix;

  private Path workDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
  private DataStreamCache imageCache;
  private int maximumKeysPerRequest = 500;
  private long downloadExpirationSeconds = TimeUnit.HOURS.toSeconds(1);

  /**
   * Constructor.
   * 
   * @param client
   *          the client to use
   * @param bucketName
   *          the S3 bucket name to use
   * @param objectKeyPrefix
   *          a folder path to prefix all object keys with
   */
  public S3NodeImageRepository(AmazonS3 client, String bucketName, String objectKeyPrefix) {
    super();
    this.client = client;
    this.bucketName = bucketName;
    this.objectKeyPrefix = objectKeyPrefix;
  }

  private String absoluteObjectKey(String objectKey) {
    String globalPrefix = this.objectKeyPrefix;
    if (globalPrefix == null) {
      return objectKey;
    }
    return globalPrefix + objectKey;
  }

  @Override
  public Iterable<SolarNodeImageInfo> findAll() {
    List<SolarNodeImageInfo> result = new ArrayList<>(20);

    final ListObjectsV2Request req = new ListObjectsV2Request();
    req.setBucketName(bucketName);
    req.setMaxKeys(maximumKeysPerRequest);
    req.setPrefix(absoluteObjectKey(META_OBJECT_KEY_PREFIX));
    ListObjectsV2Result listResult;
    do {
      listResult = client.listObjectsV2(req);

      for (S3ObjectSummary objectSummary : listResult.getObjectSummaries()) {
        if (!objectSummary.getKey().endsWith(METADATA_OBJECT_KEY_SUFFIX)) {
          continue;
        }
        String id = StringUtils.getFilename(objectSummary.getKey().substring(0,
            objectSummary.getKey().length() - METADATA_OBJECT_KEY_SUFFIX.length()));

        result.add(new S3SolarNodeImage(id, objectSummary.getBucketName(), objectSummary.getKey(),
            absoluteObjectKey(DATA_OBJECT_KEY_PREFIX + id), client, imageCache));
      }
      req.setContinuationToken(listResult.getNextContinuationToken());
    } while (listResult.isTruncated() == true);

    return result;
  }

  @Override
  public SolarNodeImage findOne(String id) {
    try {
      return findOneInternal(id);
    } catch (IOException e) {
      throw new RuntimeException("Error getting image " + id + ": " + e.getMessage(), e);
    }
  }

  private S3SolarNodeImage findOneInternal(String id) throws IOException {
    final String metaObjectKey = absoluteObjectKey(
        META_OBJECT_KEY_PREFIX + id + METADATA_OBJECT_KEY_SUFFIX);
    final String imageObjectKey = MaxCompressorStreamFactory.getCompressedFilename(
        getCompressionType(),
        absoluteObjectKey(DATA_OBJECT_KEY_PREFIX + id + IMAGE_OBJECT_KEY_SUFFIX));
    S3Object object = client.getObject(bucketName, metaObjectKey);
    return new S3SolarNodeImage(id, object, imageObjectKey, client, imageCache);
  }

  @Override
  public String getDownloadUrl(SolarNodeImage image) {
    final String imageObjectKey = MaxCompressorStreamFactory.getCompressedFilename(
        getCompressionType(),
        absoluteObjectKey(DATA_OBJECT_KEY_PREFIX + image.getId() + IMAGE_OBJECT_KEY_SUFFIX));
    Date expiration = new Date(
        System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(downloadExpirationSeconds));
    return client.generatePresignedUrl(bucketName, imageObjectKey, expiration).toString();
  }

  @Override
  public int getSaveTaskStepCount() {
    return 3; // compress, upload data, upload meta
  }

  @Override
  public SolarNodeImage save(SolarNodeImage image, TaskStepTracker tracker) {
    final String id = image.getId();
    final String metaObjectKey = absoluteObjectKey(
        META_OBJECT_KEY_PREFIX + id + METADATA_OBJECT_KEY_SUFFIX);
    final String imageObjectKey = MaxCompressorStreamFactory.getCompressedFilename(
        getCompressionType(),
        absoluteObjectKey(DATA_OBJECT_KEY_PREFIX + id + IMAGE_OBJECT_KEY_SUFFIX));

    final Path file;
    try {
      file = Files.createTempFile(workDirectory, "node-image-", "");
    } catch (IOException e) {
      throw new RuntimeException("Error creating temporary image data file", e);
    }

    // compute the digests of both the input and output streams while copying...
    final long expectedInputContentLength = image.getUncompressedContentLength();
    MessageDigest inputDigest = DigestUtils.getSha256Digest();
    MessageDigest outputDigest = DigestUtils.getSha256Digest();

    MutableLong inputContentLength = new MutableLong(0);
    MutableLong outputContentLength = new MutableLong(0);

    try {
      try (InputStream in = image.getInputStream();
          OutputStream out = new BufferedOutputStream(new FileOutputStream(file.toFile()))) {
        log.info("Compressing image {} to {} using {} @ {}%", image.getFilename(), file,
            getCompressionType(), (int) (getCompressionRatio() * 100));
        FileCopyUtils.copy(in,
            new TaskStepTrackerOutputStream(expectedInputContentLength, tracker,
                new MessageDigestOutputStream(inputDigest, inputContentLength,
                    createCompressorOutputStream(
                        new MessageDigestOutputStream(outputDigest, outputContentLength, out)))));
        tracker.completeStep(); // step 1
      } catch (CompressorException | IOException e) {
        throw new RuntimeException("Error compressing image data to " + file, e);
      }

      try (InputStream in = new TaskStepTrackerInputStream(outputContentLength.longValue(), tracker,
          new BufferedInputStream(Files.newInputStream(file)))) {
        log.info("Uploading image {} to {}", file, imageObjectKey);
        tracker.setMessage("Uploading customized image");
        ObjectMetadata imageObjectMeta = new ObjectMetadata();
        imageObjectMeta.setContentLength(outputContentLength.longValue());
        PutObjectRequest req = new PutObjectRequest(bucketName, imageObjectKey, in,
            imageObjectMeta);
        client.putObject(req);
        tracker.completeStep(); // step 2
      } catch (IOException e) {
        throw new RuntimeException("Error uploading image data to " + imageObjectKey, e);
      }

      try {
        SolarNodeImageInfo info = new BasicSolarNodeImageInfo(id,
            new String(Hex.encodeHex(outputDigest.digest())), outputContentLength.longValue(),
            new String(Hex.encodeHex(inputDigest.digest())), inputContentLength.longValue());
        byte[] infoJson = OBJECT_MAPPER.writeValueAsBytes(info);
        ObjectMetadata metaObjectMeta = new ObjectMetadata();
        metaObjectMeta.setContentLength(infoJson.length);
        metaObjectMeta.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        try (InputStream in = new TaskStepTrackerInputStream(infoJson.length, tracker,
            new ByteArrayInputStream(infoJson))) {
          client.putObject(bucketName, metaObjectKey, in, metaObjectMeta);
          tracker.completeStep(); // step 3
        }
        return findOne(id);
      } catch (IOException e) {
        throw new RuntimeException("Error writing image metadata to " + metaObjectKey, e);
      }
    } finally {
      try {
        Files.deleteIfExists(file);
      } catch (IOException e) {
        log.warn("Error deleting temporary image file " + file + ": " + e.getMessage());
      }
    }
  }

  @Override
  public void delete(String id) {
    final String metaObjectKey = absoluteObjectKey(
        META_OBJECT_KEY_PREFIX + id + METADATA_OBJECT_KEY_SUFFIX);
    final String imageObjectKey = MaxCompressorStreamFactory.getCompressedFilename(
        getCompressionType(),
        absoluteObjectKey(DATA_OBJECT_KEY_PREFIX + id + IMAGE_OBJECT_KEY_SUFFIX));
    DeleteObjectsRequest req = new DeleteObjectsRequest(bucketName).withKeys(metaObjectKey,
        imageObjectKey);
    client.deleteObjects(req);
  }

  /**
   * Set the maximum number of S3 object keys to request in one request.
   * 
   * @param maximumKeysPerRequest
   *          the maximum to set
   */
  public void setMaximumKeysPerRequest(int maximumKeysPerRequest) {
    this.maximumKeysPerRequest = maximumKeysPerRequest;
  }

  /**
   * Set a cache to use for caching S3 image objects.
   * 
   * @param imageCache
   *          a cache
   */
  public void setImageCache(DataStreamCache imageCache) {
    this.imageCache = imageCache;
  }

  /**
   * Set a directory to store temporary data files in.
   * 
   * <p>
   * This directory will be used to hold compressed images files, so the file system must be large
   * enough to hold that data.
   * </p>
   * 
   * @param workDirectory
   *          the work directory to use
   */
  public void setWorkDirectory(Path workDirectory) {
    this.workDirectory = workDirectory;
  }

  /**
   * Set the number of seconds for download links to be valid for.
   * 
   * @param downloadExpirationSeconds
   *          the number of seconds; defaults to 1 hour
   */
  public void setDownloadExpirationSeconds(long downloadExpirationSeconds) {
    this.downloadExpirationSeconds = downloadExpirationSeconds;
  }

}
