/* ==================================================================
 * CachingFileInputStream.java - 22/10/2017 2:40:25 PM
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;

import org.apache.commons.io.input.TeeInputStream;

/**
 * {@link InputStream} that copies all bytes to a file while reading, so they can be retrieved again
 * later.
 * 
 * <p>
 * This class will copy the stream to a temporary file, and then rename the temporary file to
 * another file once the stream is fully copied.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CachingFileInputStream extends TeeInputStream {

  private final Path temporaryCacheFile;
  private final Path cacheFile;
  private final Lock lock;
  private boolean complete = false;

  /**
   * Constructor.
   * 
   * @param in
   *          the stream to copy
   * @param temporaryCacheFile
   *          a temporary file to copy the stream to
   * @param cacheFile
   *          the final file to move the temporary file to once the stream has been read completely
   * @param lock
   *          a lock to release once the final final has been created successfully
   * @throws IOException
   *           if an IO error occurs creating the output stream to {@code temporaryCacheFile}
   */
  public CachingFileInputStream(InputStream in, Path temporaryCacheFile, Path cacheFile, Lock lock)
      throws IOException {
    super(in, new BufferedOutputStream(
        Files.newOutputStream(temporaryCacheFile, StandardOpenOption.WRITE)), false);
    this.temporaryCacheFile = temporaryCacheFile;
    this.cacheFile = cacheFile;
    this.lock = lock;
  }

  @Override
  public int read() throws IOException {
    int b = super.read();
    if (b < 0) {
      completeCacheFile();
    }
    return b;
  }

  @Override
  public int read(byte[] b) throws IOException {
    int count = super.read(b);
    if (count < 0) {
      completeCacheFile();
    }
    return count;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int count = super.read(b, off, len);
    if (count < 0) {
      completeCacheFile();
    }
    return count;
  }

  private void completeCacheFile() throws IOException {
    try {
      Files.move(temporaryCacheFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      complete = true;
      lock.unlock();
    }
  }

  @Override
  protected void finalize() {
    if (!complete) {
      try {
        Files.deleteIfExists(temporaryCacheFile);
      } catch (Exception e) {
        // ignore
      } finally {
        lock.unlock();
      }
    }
  }

}
