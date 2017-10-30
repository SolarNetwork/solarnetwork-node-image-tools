/* ==================================================================
 * NodeImageController.java - 18/10/2017 7:42:24 AM
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

package net.solarnetwork.nim.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiOperation;
import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.domain.SolarNodeImageOptions;
import net.solarnetwork.nim.domain.SolarNodeImageReceipt;
import net.solarnetwork.nim.domain.SolarNodeImageResource;
import net.solarnetwork.nim.service.NodeImageRepository;
import net.solarnetwork.nim.service.NodeImageService;
import net.solarnetwork.web.domain.Response;

/**
 * Web controller for node image operations.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/images")
public class NodeImageController {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final NodeImageRepository nodeImageRepo;
  private final NodeImageService nodeImageService;

  /**
   * Constructor.
   * 
   * @param nodeImageRepo
   *          the repository of base node images
   * @param nodeImageService
   *          the service for customizing the images with
   */
  @Autowired
  public NodeImageController(@Qualifier("source") NodeImageRepository nodeImageRepo,
      NodeImageService nodeImageService) {
    super();
    this.nodeImageRepo = nodeImageRepo;
    this.nodeImageService = nodeImageService;
  }

  @GetMapping("/infos")
  @ApiOperation(value = "", notes = "Get all available image info.")
  public Response<List<SolarNodeImageInfo>> allImages() {
    Iterable<SolarNodeImageInfo> iterable = nodeImageRepo.findAll();
    List<SolarNodeImageInfo> result = StreamSupport.stream(iterable.spliterator(), false)
        .collect(Collectors.toList());
    return Response.response(result);
  }

  @PostMapping("/authorize")
  public Response<String> authorize(
      @RequestHeader("X-SN-PreSignedAuthorization") String authorization,
      @RequestHeader("X-SN-Date") Date authorizationDate) {
    String key = nodeImageService.authorize(authorization, authorizationDate);
    return Response.response(key);
  }

  /**
   * Customize an image.
   * 
   * @param imageId
   *          the ID of the base image to customize
   * @param key
   *          a unique key that is required for future checks on the result
   * @param dataFiles
   *          a set of data file resources to customize the image with
   * @param optionsFile
   *          a JSON encoded {@link SolarNodeImageOptions} object to pass to the image customization
   *          task
   * @return a receipt
   */
  @PostMapping(value = "/create/{imageId}/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Response<SolarNodeImageReceipt> customizeImage(@PathVariable("imageId") String imageId,
      @PathVariable("key") String key, @RequestParam("dataFile") MultipartFile[] dataFiles,
      @RequestParam(name = "options", required = false) MultipartFile optionsFile)
      throws IOException {
    List<SolarNodeImageResource> resources = new ArrayList<>(dataFiles.length);
    for (MultipartFile dataFile : dataFiles) {
      resources.add(new MultipartFileSolarNodeImageResource(dataFile));
    }
    SolarNodeImageOptions options = null;
    if (optionsFile != null) {
      options = OBJECT_MAPPER.readValue(optionsFile.getInputStream(), SolarNodeImageOptions.class);
    }
    SolarNodeImage image = nodeImageRepo.findOne(imageId);
    try {
      SolarNodeImageReceipt receipt = nodeImageService.createImage(key, image, resources, options);
      return Response.response(receipt);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get a custom image creation receipt.
   * 
   * @param key
   *          the same unique key previously passed to
   *          {@link #customizeImage(String, String, MultipartFile[])}
   * @param receiptId
   *          the ID of the receipt to get
   * @return the receipt
   */
  @GetMapping("/receipt/{receiptId}/{key}")
  public Response<SolarNodeImageReceipt> getReceipt(@PathVariable("key") String key,
      @PathVariable("receiptId") String receiptId) {
    SolarNodeImageReceipt receipt = nodeImageService.getReceipt(key, receiptId);
    return Response.response(receipt);
  }

  /**
   * Get the contents of an image.
   * 
   * @param key
   *          the same unique key previously passed to
   *          {@link #customizeImage(String, String, MultipartFile[])}
   * @param receiptId
   *          the ID of the receipt to get
   * @return the image data
   */
  @GetMapping("/{receiptId}/{key}")
  public ResponseEntity<Resource> getImageFile(@PathVariable("key") String key,
      @PathVariable("receiptId") String receiptId) {
    SolarNodeImageReceipt receipt = nodeImageService.getReceipt(key, receiptId);
    if (receipt == null) {
      return ResponseEntity.notFound().build();
    }

    if (!receipt.isDone()) {
      // image not ready; return 503 + Retry-AFter
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .header(HttpHeaders.RETRY_AFTER, "60").build();
    }

    try {
      SolarNodeImage image = receipt.get();
      BodyBuilder builder = ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + image.getFilename())
          .contentType(MediaType.APPLICATION_OCTET_STREAM);
      long length = image.getContentLength();
      if (length > 0) {
        builder.contentLength(length);
      }
      return builder.body(new InputStreamResource(image.getInputStream()));
    } catch (ExecutionException | InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
