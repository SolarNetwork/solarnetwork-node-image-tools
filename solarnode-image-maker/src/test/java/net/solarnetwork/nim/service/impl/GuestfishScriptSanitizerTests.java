/* ==================================================================
 * GuestfishScriptSanitizerTests.java - 21/10/2017 7:04:05 AM
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import net.solarnetwork.nim.NodeImageScriptException;

/**
 * Test cases for the {@link GuestfishScriptSanitizer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GuestfishScriptSanitizerTests {

  @Test
  public void noProblems() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-01.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    sanitizer.validate(scriptFile);
  }

  @Test
  public void localCommandRestricted() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-02.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    try {
      sanitizer.validate(scriptFile);
      Assert.fail("Expected NodeImageScriptException exception");
    } catch (NodeImageScriptException e) {
      assertThat(e.getScriptName(), equalTo("sanitizer-test-02.fish"));
      assertThat(e.getLineNumber(), equalTo(2));
    }
  }

  @Test
  public void localCommandWithWhitespaceRestricted() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-03.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    try {
      sanitizer.validate(scriptFile);
      Assert.fail("Expected NodeImageScriptException exception");
    } catch (NodeImageScriptException e) {
      assertThat(e.getScriptName(), equalTo("sanitizer-test-03.fish"));
      assertThat(e.getLineNumber(), equalTo(2));
    }
  }

  @Test
  public void localRedirectedCommandRestricted() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-04.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    try {
      sanitizer.validate(scriptFile);
      Assert.fail("Expected NodeImageScriptException exception");
    } catch (NodeImageScriptException e) {
      assertThat(e.getScriptName(), equalTo("sanitizer-test-04.fish"));
      assertThat(e.getLineNumber(), equalTo(2));
    }
  }

  @Test
  public void localRedirectedCommandWithWhitespaceRestricted() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-05.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    try {
      sanitizer.validate(scriptFile);
      Assert.fail("Expected NodeImageScriptException exception");
    } catch (NodeImageScriptException e) {
      assertThat(e.getScriptName(), equalTo("sanitizer-test-05.fish"));
      assertThat(e.getLineNumber(), equalTo(2));
    }
  }

  @Test
  public void lcdCommandRestricted() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-06.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    try {
      sanitizer.validate(scriptFile);
      Assert.fail("Expected NodeImageScriptException exception");
    } catch (NodeImageScriptException e) {
      assertThat(e.getScriptName(), equalTo("sanitizer-test-06.fish"));
      assertThat(e.getLineNumber(), equalTo(2));
    }
  }

  @Test
  public void lcdWithWhitespaceRestricted() throws Exception {
    URL scriptUrl = getClass().getResource("sanitizer-test-07.fish");
    Path scriptFile = Paths.get(scriptUrl.toURI());
    GuestfishScriptSanitizer sanitizer = new GuestfishScriptSanitizer();
    try {
      sanitizer.validate(scriptFile);
      Assert.fail("Expected NodeImageScriptException exception");
    } catch (NodeImageScriptException e) {
      assertThat(e.getScriptName(), equalTo("sanitizer-test-07.fish"));
      assertThat(e.getLineNumber(), equalTo(2));
    }
  }

}
