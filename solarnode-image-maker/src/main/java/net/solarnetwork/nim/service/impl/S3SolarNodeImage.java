/* ==================================================================
 * S3SolarNodeImage.java - 21/10/2017 5:04:31 PM
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
import java.io.InputStream;
import java.util.function.Supplier;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.annotation.JsonIgnore;

import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.service.DataStreamCache;

/**
 * S3 backed {@link SolarNodeImage}.
 * 
 * @author matt
 * @version 1.0
 */
public class S3SolarNodeImage implements SolarNodeImage {

  private final String id;
  private final String bucketName;
  private final String objectKey;
  private final String imageObjectKey;
  private final String filename;
  private final AmazonS3 client;
  private final DataStreamCache imageCache;

  private BasicSolarNodeImageInfo info = null;

  /**
   * Constructor.
   * 
   * @param id
   *          the ID of the image
   * @param bucketName
   *          the S3 bucket name to use
   * @param objectKey
   *          the S3 object key for the metadata
   * @param imageObjectKey
   *          the S3 object key for the data
   * @param client
   *          the S3 client
   * @param imageCache
   *          a cache to use for the image data
   */
  public S3SolarNodeImage(String id, String bucketName, String objectKey, String imageObjectKey,
      AmazonS3 client, DataStreamCache imageCache) {
    super();
    this.id = id;
    this.bucketName = bucketName;
    this.objectKey = objectKey;
    this.imageObjectKey = imageObjectKey;
    this.filename = filenameFromObjectKey(imageObjectKey);
    this.client = client;
    this.imageCache = imageCache;
  }

  /**
   * Construct from a {@link S3Object} of the metadata.
   * 
   * @param id
   *          the ID of the image
   * @param object
   *          the metadata object
   * @param imageObjectKey
   *          the S3 object key of the image data
   * @param client
   *          the S3 client
   * @param imageCache
   *          a cache to use for the image data
   */
  public S3SolarNodeImage(String id, S3Object object, String imageObjectKey, AmazonS3 client,
      DataStreamCache imageCache) {
    super();
    this.id = id;
    this.bucketName = object.getBucketName();
    this.objectKey = object.getKey();
    this.imageObjectKey = imageObjectKey;
    this.filename = filenameFromObjectKey(imageObjectKey);
    this.client = client;
    this.imageCache = imageCache;
    try {
      this.info = S3NodeImageRepository.OBJECT_MAPPER.readValue(object.getObjectContent(),
          BasicSolarNodeImageInfo.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String filenameFromObjectKey(String imageObjectKey) {
    int lastSlashIdx = imageObjectKey.lastIndexOf('/');
    if (lastSlashIdx > 0 && lastSlashIdx + 1 < imageObjectKey.length()) {
      return imageObjectKey.substring(lastSlashIdx + 1);
    }
    return imageObjectKey;
  }

  @Override
  public String getId() {
    return id;
  }

  private synchronized SolarNodeImageInfo getInfo() {
    if (info != null) {
      return info;
    }
    try {
      info = S3NodeImageRepository.OBJECT_MAPPER.readValue(
          client.getObjectAsString(bucketName, objectKey), BasicSolarNodeImageInfo.class);
      return info;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonIgnore
  @Override
  public InputStream getInputStream() throws IOException {
    Supplier<InputStream> s = () -> client.getObject(bucketName, imageObjectKey).getObjectContent();
    if (imageCache != null) {
      return imageCache.get(id, s);
    }
    return s.get();
  }

  @Override
  public String getSha256() {
    return getInfo().getSha256();
  }

  @Override
  public long getContentLength() {
    return getInfo().getContentLength();
  }

  @Override
  public String getUncompressedSha256() {
    return getInfo().getUncompressedSha256();
  }

  @Override
  public long getUncompressedContentLength() {
    return getInfo().getUncompressedContentLength();
  }

  @Override
  public String getFilename() {
    return filename;
  }

}
