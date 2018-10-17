/* ==================================================================
 * FileSystemNodeImageRepositoryTests.java - 17/10/2017 5:44:54 PM
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

import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

import net.solarnetwork.nim.domain.SolarNodeImage;
import net.solarnetwork.nim.domain.SolarNodeImageInfo;
import net.solarnetwork.nim.util.DecompressingSolarNodeImage;

/**
 * Test cases for the {@link FileSystemNodeImageRepository} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FileSystemNodeImageRepositoryTests {

  private FileSystemNodeImageRepository repo;

  @Before
  public void setup() throws URISyntaxException {
    URL foobarUrl = getClass().getResource("repo/foobar.json");
    Path root = Paths.get(foobarUrl.toURI()).getParent();
    repo = new FileSystemNodeImageRepository(root);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listAll() {
    Iterable<SolarNodeImageInfo> result = repo.findAll();
    assertThat("Never null", result, notNullValue());
    assertThat("Image list", result,
        contains(pojo(SolarNodeImageInfo.class).withProperty("id", is("foobar")),
            pojo(SolarNodeImageInfo.class).withProperty("id", is("zebra"))));
  }

  @Test
  public void getImage() throws IOException {
    SolarNodeImage image = repo.findOne("foobar");
    assertThat("Image ID", image.getId(), is("foobar"));
    String data = FileCopyUtils.copyToString(
        new InputStreamReader(new DecompressingSolarNodeImage(image).getInputStream(), "UTF-8"));
    assertThat("Image contents", data, is("Hello, world."));
  }

  @Test
  public void getImageNotFound() throws IOException {
    SolarNodeImage image = repo.findOne("does-not-exist");
    assertThat(image, nullValue());
  }

}
