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
import net.solarnetwork.nim.domain.SolarNodeImageReceipt;

/**
 * Implementation of {@link SolarNodeImageReceipt} that delegates to another {@link Future}.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeImageReceiptFuture implements SolarNodeImageReceipt {

  private final long created;
  private final String id;
  private final Future<SolarNodeImage> future;

  /**
   * Constructor.
   * 
   * @param id
   *          the receipt ID
   * @param future
   *          the task
   */
  public SolarNodeImageReceiptFuture(String id, Future<SolarNodeImage> future) {
    super();
    this.created = System.currentTimeMillis();
    this.id = id;
    this.future = future;
  }

  /**
   * Get the creation date.
   * 
   * @return the creation date
   */
  public long getCreated() {
    return created;
  }

  @Override
  public String getId() {
    return id;
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
  public SolarNodeImage get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  @Override
  public SolarNodeImage get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(timeout, unit);
  }

}
