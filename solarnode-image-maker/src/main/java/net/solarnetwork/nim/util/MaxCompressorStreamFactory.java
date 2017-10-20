/* ==================================================================
 * MaxCompressorStreamFactory.java - 19/10/2017 5:35:41 PM
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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.compressors.lzma.LZMAUtils;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZUtils;

/**
 * Extension of {@link CompressorStreamFactory} to use maximum compression.
 * 
 * <p>
 * By default maximum compression is requested, however the actual amount can be changed by passing
 * a {@code compressionRatio} value < {@literal 1} to an appropriate constructor.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class MaxCompressorStreamFactory extends CompressorStreamFactory {

  private final float compressionRatio;

  /**
   * Default constructor.
   * 
   * <p>
   * Constructs with a ratio of 1.
   * </p>
   */
  public MaxCompressorStreamFactory() {
    this(1.0f);
  }

  /**
   * Construct with a desired compression ratio.
   * 
   * @param compressionRatio
   *          a value between {@literal 0} (least compression, fastest operation) and {@literal 1}
   *          (most compression, slowest operation)
   */
  public MaxCompressorStreamFactory(float compressionRatio) {
    super();
    this.compressionRatio = compressionRatio;
  }

  public MaxCompressorStreamFactory(boolean decompressUntilEof) {
    super(decompressUntilEof);
    this.compressionRatio = 1f;
  }

  public MaxCompressorStreamFactory(boolean decompressUntilEof, int memoryLimitInKb,
      float compressionRatio) {
    super(decompressUntilEof, memoryLimitInKb);
    this.compressionRatio = compressionRatio;
  }

  @Override
  public CompressorOutputStream createCompressorOutputStream(String name, OutputStream out)
      throws CompressorException {
    try {
      if (XZ.equals(name)) {
        int rate = Math.max(1, Math.round(compressionRatio * 9f));
        return new XZCompressorOutputStream(out, rate);
      }
    } catch (final IOException e) {
      throw new CompressorException("Could not create CompressorOutputStream", e);
    }
    return super.createCompressorOutputStream(name, out);
  }

  /**
   * Maps the given name of a compressed file to the name that the file should have after
   * uncompression.
   * 
   * <p>
   * Commonly used file type specific suffixes like ".tbz" or ".tbz2" are automatically detected and
   * correctly mapped. For example the name "package.tbz2" is mapped to "package.tar". If no
   * compression suffix is detected, then the filename is returned unmapped.
   * </p>
   *
   * @param name
   *          the compressor name, i.e. {@value #GZIP}, {@value #BZIP2}, {@value #XZ},
   *          {@value #PACK200}, {@value #SNAPPY_FRAMED}, {@value #LZ4_BLOCK}, {@value #LZ4_FRAMED}
   *          or {@value #DEFLATE}; if {@literal null} then try to auto-detect the compression type
   *          from the {@code filename}
   * @param filename
   *          name of a file
   * @return name of the corresponding uncompressed file
   */
  public static String getUncompressedFilename(final String name, final String filename) {
    if (name != null) {
      switch (name) {
        case BZIP2:
          return BZip2Utils.getUncompressedFilename(filename);
        case GZIP:
          return GzipUtils.getUncompressedFilename(filename);
        case LZMA:
          return LZMAUtils.getUncompressedFilename(filename);
        case XZ:
          return XZUtils.getUncompressedFilename(filename);
        default:
          // fall below
          break;
      }
    }
    if (BZip2Utils.isCompressedFilename(filename)) {
      return BZip2Utils.getUncompressedFilename(filename);
    } else if (GzipUtils.isCompressedFilename(filename)) {
      return GzipUtils.getUncompressedFilename(filename);
    } else if (LZMAUtils.isCompressedFilename(filename)) {
      return LZMAUtils.getUncompressedFilename(filename);
    } else if (XZUtils.isCompressedFilename(filename)) {
      return XZUtils.getUncompressedFilename(filename);
    }
    String nameExt = "." + name;
    if (filename.toLowerCase().endsWith(nameExt)) {
      return filename.substring(0, filename.length() - nameExt.length());
    }
    return filename;
  }

  /**
   * Maps the given filename to the name that the file should have after compression with the given
   * type.
   * 
   * <p>
   * Common file types with custom suffixes for compressed versions are automatically detected and
   * correctly mapped. For example the name "package.tar" is mapped to "package.txz" for XZ
   * compression. If no custom mapping is applicable, then a default extension matching {@code name}
   * is appended to the filename.
   * </p>
   * 
   * @param name
   *          the compressor name, i.e. {@value #GZIP}, {@value #BZIP2}, {@value #XZ},
   *          {@value #PACK200}, {@value #SNAPPY_FRAMED}, {@value #LZ4_BLOCK}, {@value #LZ4_FRAMED}
   *          or {@value #DEFLATE}
   * @param filename
   *          name of a file
   * @return name of the corresponding compressed file
   */
  public static String getCompressedFilename(final String name, final String filename) {
    switch (name) {
      case BZIP2:
        return BZip2Utils.getCompressedFilename(filename);
      case GZIP:
        return GzipUtils.getCompressedFilename(filename);
      case LZMA:
        return LZMAUtils.getCompressedFilename(filename);
      case XZ:
        return XZUtils.getCompressedFilename(filename);
      default:
        return filename + "." + name;
    }
  }

}
