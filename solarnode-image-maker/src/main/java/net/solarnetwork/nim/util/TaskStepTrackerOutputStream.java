/* ==================================================================
 * TaskStepTrackerOutputStream.java - 20/10/2017 9:44:52 AM
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class TaskStepTrackerOutputStream extends FilterOutputStream {

  private final long expected;
  private final TaskStepTracker tracker;
  private long count = 0;

  /**
   * Constructor.
   */
  public TaskStepTrackerOutputStream(long expectedLength, TaskStepTracker tracker,
      OutputStream out) {
    super(out);
    this.expected = expectedLength;
    this.tracker = tracker;
  }

  @Override
  public void write(byte[] buf, int offset, int len) throws IOException {
    out.write(buf, offset, len);
    count += len;
    updateTracker();
  }

  @Override
  public void write(byte[] buf) throws IOException {
    out.write(buf);
    count += buf.length;
    updateTracker();
  }

  @Override
  public void write(int b) throws IOException {
    out.write(b);
    count += 1;
    updateTracker();
  }

  private void updateTracker() {
    tracker.setStepPercentComplete((double) count / (double) expected);
  }

}
