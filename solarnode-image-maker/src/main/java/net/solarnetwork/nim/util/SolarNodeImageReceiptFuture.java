/* ==================================================================
 * SolarNodeImageReceiptFuture.java - 19/10/2017 7:30:51 AM
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

package net.solarnetwork.nim.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.domain.SolarNodeImageReceipt;

/**
 * Implementation of {@link SolarNodeImageReceipt} that delegates to another {@link Future}.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeImageReceiptFuture implements SolarNodeImageReceipt {

  private final long createdDate;
  private final String id;
  private final String baseImageId;
  private final Future<SolarNodeImage> future;
  private final TaskStepTracker tracker;

  /**
   * Constructor.
   * 
   * @param id
   *          the receipt ID
   * @param baseImageId
   *          the base image ID
   * @param future
   *          the task
   * @param tracker
   *          a step tracker
   */
  public SolarNodeImageReceiptFuture(String id, String baseImageId, Future<SolarNodeImage> future,
      TaskStepTracker tracker) {
    super();
    this.createdDate = System.currentTimeMillis();
    this.id = id;
    this.baseImageId = baseImageId;
    this.future = future;
    this.tracker = tracker;
  }

  @Override
  public long getCreatedDate() {
    return createdDate;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getBaseImageId() {
    return baseImageId;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return future.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public boolean isStarted() {
    return tracker.isStarted();
  }

  @Override
  public Long getStartedDate() {
    return tracker.getStartedDate();
  }

  @Override
  public SolarNodeImage get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  @Override
  public SolarNodeImage get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(timeout, unit);
  }

  @Override
  public String getMessage() {
    return tracker.getMessage();
  }

  @Override
  public double getPercentComplete() {
    return tracker.getOverallPercentComplete();
  }

  @Override
  public SolarNodeImageInfo getImageInfo() {
    if (isDone()) {
      try {
        return get();
      } catch (Exception e) {
        tracker.setMessage(e.getMessage());
      }
    }
    return null;
  }

  @Override
  public Long getCompletedDate() {
    return tracker.getCompletedDate();
  }

  @Override
  public final String getDownloadUrl() {
    if (!isDone() || isCancelled() || getPercentComplete() < 1f) {
      return null;
    }
    try {
      return getDownloadUrlInternal(get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Hook for a generated download URL.
   * 
   * <p>
   * This method returns {@literal null}. Extending classes can provide an actual value.
   * </p>
   * 
   * @param image
   *          the image
   * @return the download URL
   */
  protected String getDownloadUrlInternal(SolarNodeImage image) {
    return null;
  }

}
