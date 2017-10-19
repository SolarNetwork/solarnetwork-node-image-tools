/* ==================================================================
 * BasicSolarNodeImageInfoTests.java - 18/10/2017 9:31:17 AM
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

package net.solarnetwork.nim.service.domain;

import static com.spotify.hamcrest.jackson.JsonMatchers.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonLong;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.nim.domain.BasicSolarNodeImageInfo;

/**
 * Test cases for the {@link BasicSolarNodeImageInfo} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicSolarNodeImageInfoTests {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

  @Test
  public void serializeAsJson() throws IOException {
    BasicSolarNodeImageInfo info = new BasicSolarNodeImageInfo("foobar", null, 1, null, 2);
    String json = OBJECT_MAPPER.writeValueAsString(info);
    // @formatter:off
    assertThat(json, isJsonStringMatching(jsonObject()
        .where("id", is(jsonText("foobar")))
        .where("contentLength", is(jsonLong(1)))
        .where("uncompressedContentLength", is(jsonLong(2)))
        ));
    // @formatter:on
  }

  @Test
  public void deserializeFromJson() throws IOException {
    BasicSolarNodeImageInfo info = OBJECT_MAPPER.readValue("{\"id\":\"foobar\"}",
        BasicSolarNodeImageInfo.class);
    assertThat(info, is(pojo(BasicSolarNodeImageInfo.class).withProperty("id", is("foobar"))));
  }

}
