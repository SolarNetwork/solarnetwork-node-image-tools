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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.DataStreamCache;
import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.service.UpdatableNodeImageRepository;
import net.solarnetwork.nim.util.DecompressingSolarNodeImage;
import net.solarnetwork.nim.util.MaxCompressorStreamFactory;
import net.solarnetwork.nim.util.TaskStepTracker;

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

  private DataStreamCache imageCache;
  private int maximumKeysPerRequest = 500;

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
      SolarNodeImage result = findOneInternal(id);
      if (result != null) {
        result = new DecompressingSolarNodeImage(result);
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException("Error getting image " + id + ": " + e.getMessage(), e);
    }
  }

  @Override
  public SolarNodeImage findOneCompressed(String id) {
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
  public SolarNodeImage save(SolarNodeImage image, TaskStepTracker tracker) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String id) {
    // TODO Auto-generated method stub

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

}
