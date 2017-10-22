/* ==================================================================
 * FileSystemDataStreamCache.java - 22/10/2017 12:56:29 PM
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import net.solarnetwork.nim.service.DataStreamCache;
import net.solarnetwork.nim.util.CachingFileInputStream;

/**
 * {@link DataStreamCache} implementation using files.
 * 
 * @author matt
 * @version 1.0
 */
public class FileSystemDataStreamCache implements DataStreamCache {

  private static final ConcurrentMap<String, Lock> LOCKS = new ConcurrentHashMap<>(8);

  private final Path cacheDirectory;

  public FileSystemDataStreamCache(Path cacheDirectory) {
    super();
    this.cacheDirectory = cacheDirectory;
  }

  @Override
  public InputStream get(String key, Supplier<InputStream> supplier) throws IOException {
    Lock lock = LOCKS.computeIfAbsent(key, k -> new ReentrantLock(true));
    lock.lock();
    return getInternal(key, supplier, lock);
  }

  private InputStream getInternal(String key, Supplier<InputStream> supplier, Lock lock)
      throws IOException {
    try {
      Path cacheFile = cacheDirectory.resolve(key);
      if (Files.isReadable(cacheFile)) {
        try {
          InputStream result = new BufferedInputStream(
              Files.newInputStream(cacheFile, StandardOpenOption.READ));
          return result;
        } finally {
          lock.unlock();
        }
      }

      // tee the input stream of the supplier to the file system
      Path tmpCacheFile = Files.createTempFile(cacheDirectory, "." + key + "-", "");
      return new CachingFileInputStream(supplier.get(), tmpCacheFile, cacheFile, lock);
    } catch (RuntimeException | Error e) {
      lock.unlock();
      throw e;
    }
  }

}
