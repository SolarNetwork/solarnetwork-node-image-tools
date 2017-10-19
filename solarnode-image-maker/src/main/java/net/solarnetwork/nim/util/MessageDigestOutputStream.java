/* ==================================================================
 * MessageDigestOutputStream.java - 20/10/2017 9:10:39 AM
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
import java.security.MessageDigest;

import org.apache.commons.lang3.mutable.MutableLong;

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
public class MessageDigestOutputStream extends FilterOutputStream {

  private final MessageDigest digest;
  private final MutableLong counter;

  /**
   * Constructor.
   */
  public MessageDigestOutputStream(MessageDigest digest, MutableLong counter, OutputStream out) {
    super(out);
    this.digest = digest;
    this.counter = counter;
  }

  @Override
  public void write(byte[] buf, int offset, int len) throws IOException {
    digest.update(buf, offset, len);
    out.write(buf, offset, len);
    counter.add(len);
  }

  @Override
  public void write(byte[] buf) throws IOException {
    digest.update(buf);
    out.write(buf);
    counter.add(buf.length);
  }

  @Override
  public void write(int b) throws IOException {
    digest.update((byte) b);
    out.write(b);
    counter.increment();
  }

}
