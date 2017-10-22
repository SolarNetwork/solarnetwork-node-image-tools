/* ==================================================================
 * MessageDigestInputStream.java - 22/10/2017 8:22:33 PM
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.apache.commons.lang3.mutable.MutableLong;

/**
 * InputStream that calculates a message digest and tracks the number of bytes read.
 * 
 * @author matt
 * @version 1.0
 */
public class MessageDigestInputStream extends FilterInputStream {

  private final MessageDigest digest;
  private final MutableLong counter;

  /**
   * Constructor.
   * 
   * @param digest
   *          the digest instance to update
   * @param counter
   *          the counter to update
   * @param out
   *          the output stream to wrap
   */
  public MessageDigestInputStream(MessageDigest digest, MutableLong counter, InputStream out) {
    super(out);
    this.digest = digest;
    this.counter = counter;
  }

  @Override
  public int read() throws IOException {
    final int b = in.read();
    if (b >= 0) {
      digest.update((byte) b);
      counter.increment();
    }
    return b;
  }

  @Override
  public int read(byte[] b) throws IOException {
    final int readLength = in.read(b);
    if (readLength > 0) {
      digest.update(b, 0, readLength);
      counter.add(readLength);
    }
    return readLength;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int readLength = in.read(b, off, len);
    if (readLength > 0) {
      digest.update(b, off, readLength);
      counter.add(readLength);
    }
    return readLength;
  }

}
