/* ==================================================================
 * SolarNetworkNodeImageAuthorizor.java - 30/10/2017 3:41:59 PM
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.nim.AuthorizationException;
import net.solarnetwork.nim.service.NodeImageAuthorizor;
import net.solarnetwork.support.HttpClientSupport;
import net.solarnetwork.util.JsonUtils;
import net.solarnetwork.web.security.WebConstants;

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
public class SolarNetworkNodeImageAuthorizor extends HttpClientSupport
    implements NodeImageAuthorizor {

  private static final Pattern SIGNED_HEADERS_PATTERN = Pattern.compile(",SignedHeaders=([^,]+),");
  private static final Pattern TOKEN_PATTERN = Pattern.compile("\\sCredential=([^,]+),");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    MAPPER.setDateFormat(sdf);
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private String apiBaseUrl = "https://data.solarnetwork.net";
  private String validateAuthenticationPath = "/solaruser/api/v1/sec/whoami";

  private static String uriHost(URI uri) {
    String host = uri.getHost();
    if (uri.getPort() != 80 && uri.getPort() != 443) {
      host += ":" + uri.getPort();
    }
    return host;
  }

  private URI apiUri(String path) {
    URI uri;
    try {
      uri = new URI(apiBaseUrl + path);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Error in configured URL settings for queuing instructions", e);
    }
    return uri;
  }

  @Override
  public Map<String, ?> authorize(String authorization, Date authorizationDate) {
    URI uri = apiUri(validateAuthenticationPath);

    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.HOST, uriHost(uri));
    headers.setDate(signedDateHeaderName(authorization), authorizationDate.getTime());
    headers.set(HttpHeaders.AUTHORIZATION, authorization);

    try {
      URLConnection conn = get(uri, MediaType.APPLICATION_JSON_UTF8_VALUE, headers);
      JsonNode node = MAPPER.readTree(getInputStreamFromURLConnection(conn));
      if (log.isTraceEnabled()) {
        log.trace("Got whoami JSON: {}", MAPPER.writeValueAsString(node));
      }
      JsonNode data = node.path("data");
      if (data.isObject()) {
        String token = null;
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(authorization);
        if (tokenMatcher.find()) {
          token = tokenMatcher.group(1);
        }
        Map<String, ?> result = JsonUtils.getStringMapFromTree(data);
        log.info("Authorized token {} via {} and received {}", token, uri, result);
        return result;
      }
      return Collections.emptyMap();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String signedDateHeaderName(String authorization) {
    if (authorization == null) {
      throw new RuntimeException("Authorization missing");
    }
    Matcher signedHeaders = SIGNED_HEADERS_PATTERN.matcher(authorization);
    if (!signedHeaders.find()) {
      throw new RuntimeException("SignedHeaders missing");
    }
    String[] signedHeaderNames = signedHeaders.group(1).split(";");
    int dateHeaderIndex = Arrays.binarySearch(signedHeaderNames, "x-sn-date");
    if (dateHeaderIndex < 0) {
      dateHeaderIndex = Arrays.binarySearch(signedHeaderNames, "date");
    }
    if (dateHeaderIndex < 0) {
      throw new RuntimeException("Date or X-SN-Date signed header name missing; available headers: "
          + signedHeaders.group(1));
    }
    String dateHeaderName;
    switch (signedHeaderNames[dateHeaderIndex]) {
      case "date":
        dateHeaderName = "Date";
        break;
      default:
        dateHeaderName = WebConstants.HEADER_DATE;
    }
    return dateHeaderName;
  }

  protected URLConnection get(URI uri, String accept, HttpHeaders headers) throws IOException {
    URLConnection conn = getURLConnection(uri.toString(), HTTP_METHOD_GET, accept);
    if (headers != null) {
      log.trace("Adding HTTP GET headers {}", headers);
      for (Map.Entry<String, String> me : headers.toSingleValueMap().entrySet()) {
        conn.setRequestProperty(me.getKey(), me.getValue());
      }
    }
    if (conn instanceof HttpURLConnection) {
      HttpURLConnection http = (HttpURLConnection) conn;
      int status = http.getResponseCode();
      if (status == 401 || status == 403) {
        throw new AuthorizationException("Authentication failure");
      }
      if (status < 200 || status > 299) {
        throw new IOException("HTTP result status not in the 200-299 range: "
            + http.getResponseCode() + " " + http.getResponseMessage());
      }
    }
    return conn;
  }

  /**
   * Set the base URL to SolarNetwork.
   * 
   * @param apiBaseUrl
   *          the base URL to use; defaults to {@code https://data.solarnetwork.net}
   */
  public void setApiBaseUrl(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  /**
   * Set the URL path to use for validating authentication.
   * 
   * @param validateAuthenticationPath
   *          the path to use; defaults to {@code /solaruser/api/v1/sec/whoami}
   */
  public void setValidateAuthenticationPath(String validateAuthenticationPath) {
    this.validateAuthenticationPath = validateAuthenticationPath;
  }

}
