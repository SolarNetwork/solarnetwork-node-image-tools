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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import net.solarnetwork.nim.service.impl.FileSystemDataStreamCache;
import net.solarnetwork.nim.service.impl.FileSystemNodeImageRepository;
import net.solarnetwork.nim.service.impl.S3NodeImageRepository;

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

  @Value("${repo.dest.compression.type:xz}")
  private String destRepoCompressionType = "xz";

  @Value("${repo.dest.compression.ratio:1}")
  private float destRepoCompressionRatio = 1f;

  @Value("${repo.source.s3.region:us-west-2}")
  private String s3SourceRepoRegion = "us-west-2";

  @Value("${repo.source.s3.bucket:#{null}}")
  private String s3SourceRepoBucketName = null;

  @Value("${repo.source.s3.objectKeyPrefix:solarnode-images/}")
  private String s3SourceRepoObjectKeyPrefix = "solarnode-images/";

  @Value("${repo.source.s3.accessKey:#{null}}")
  private String s3SourceRepoAccessKey = null;

  @Value("${repo.source.s3.secretKey:#{null}}")
  private String s3SourceRepoSecretKey = null;

  @Value("${repo.source.s3.cache.path:/var/tmp/node-image-cache}")
  private File s3SourceRepoCacheDirectory = new File("/var/tmp/node-image-cache");

  @Value("${repo.dest.s3.region:us-west-2}")
  private String s3DestRepoRegion = "us-west-2";

  @Value("${repo.dest.s3.bucket:solarnetwork-dev-testing}")
  private String s3DestRepoBucketName = null;

  @Value("${repo.dest.s3.objectKeyPrefix:solarnode-custom-images/}")
  private String s3DestObjectKeyPrefix = "solarnode-custom-images/";

  @Value("${repo.dest.s3.accessKey:#{null}}")
  private String s3DestRepoAccessKey = null;

  @Value("${repo.dest.s3.secretKey:#{null}}")
  private String s3DestRepoSecretKey = null;

  @Value("${repo.dest.s3.work.path:#{systemProperties['java.io.tmpdir']}}")
  private File s3DestRepoWorkDirectory = new File(System.getProperty("java.io.tmpdir"));

  /**
   * The source repository to pull base images from.
   * 
   * @return the source image repo
   */
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

  /**
   * The repository to publish the customized images to for later download.
   * 
   * @return the destination image repo
   */
  @Bean
  @Profile({ "default", "development" })
  @Qualifier("dest")
  public FileSystemNodeImageRepository fsDestNodeImageRepository() {
    if (!fsDestRepoRootDirectory.isDirectory()) {
      if (!fsDestRepoRootDirectory.mkdirs()) {
        throw new RuntimeException("FS dest repo root " + fsDestRepoRootDirectory.getAbsolutePath()
            + " does not exist and unable to create");
      }
    }
    FileSystemNodeImageRepository repo = new FileSystemNodeImageRepository(
        fsDestRepoRootDirectory.toPath());
    repo.setCompressionRatio(destRepoCompressionRatio);
    repo.setCompressionType(destRepoCompressionType);
    return repo;
  }

  /**
   * The S3 client to use for the source repository.
   * 
   * @return the S3 client
   */
  @Bean
  @Profile({ "staging", "production" })
  @Qualifier("source")
  public AmazonS3 s3SourceClient() {
    AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(s3SourceRepoRegion);
    if (s3SourceRepoAccessKey != null && s3SourceRepoSecretKey != null) {
      builder = builder.withCredentials(new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(s3SourceRepoAccessKey, s3SourceRepoSecretKey)));
    }
    return builder.build();
  }

  /**
   * The S3 source repository to pull base images from.
   * 
   * @return the source image repo
   */
  @Bean
  @Profile({ "staging", "production" })
  @Qualifier("source")
  public S3NodeImageRepository s3SourceNodeImageRepository() {
    AmazonS3 client = s3SourceClient();
    S3NodeImageRepository repo = new S3NodeImageRepository(client, s3SourceRepoBucketName,
        s3SourceRepoObjectKeyPrefix);

    if (!s3SourceRepoCacheDirectory.isDirectory()) {
      if (!s3SourceRepoCacheDirectory.mkdirs()) {
        throw new RuntimeException(
            "S3 src repo cache dir " + s3SourceRepoCacheDirectory.getAbsolutePath()
                + " does not exist and unable to create");
      }
    }

    FileSystemDataStreamCache imageCache = new FileSystemDataStreamCache(
        s3SourceRepoCacheDirectory.toPath());
    repo.setImageCache(imageCache);

    return repo;
  }

  /**
   * The S3 repository to publish the customized images to for later download.
   * 
   * @return the destination image repo
   */
  @Bean
  @Profile({ "staging", "production" })
  @Qualifier("dest")
  public S3NodeImageRepository s3DestNodeImageRepository() {
    AmazonS3 client;
    if (s3DestRepoRegion.equals(s3SourceRepoRegion)
        && s3DestRepoBucketName.equals(s3SourceRepoBucketName) && s3DestRepoAccessKey == null) {
      // reuse source S3 client for dest repo
      client = s3SourceClient();
    } else {
      client = AmazonS3ClientBuilder.standard().withRegion(s3DestRepoRegion)
          .withCredentials(new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(s3DestRepoAccessKey, s3DestRepoSecretKey)))
          .build();
    }
    S3NodeImageRepository repo = new S3NodeImageRepository(client, s3DestRepoBucketName,
        s3DestObjectKeyPrefix);
    repo.setCompressionRatio(destRepoCompressionRatio);
    repo.setCompressionType(destRepoCompressionType);
    repo.setWorkDirectory(s3DestRepoWorkDirectory.toPath());
    return repo;
  }

}
