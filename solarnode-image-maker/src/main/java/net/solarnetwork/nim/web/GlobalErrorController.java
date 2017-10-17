/* ==================================================================
 * GlobalErrorController.java - 18/10/2017 7:20:09 AM
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

package net.solarnetwork.nim.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import net.solarnetwork.web.domain.Response;

/**
 * Global error controller for otherwise unhandled MVC errors.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
@RequestMapping(path = GlobalErrorController.ERROR_PATH)
public class GlobalErrorController implements ErrorController {

  public static final String ERROR_PATH = "/error";

  private final ErrorAttributes errorAttributes;
  private final boolean debug;

  /**
   * Constructor.
   * 
   * @param errorAttributes
   *          the attributes
   * @param debug
   *          if {@literal true} then include stack traces
   */
  @Autowired
  public GlobalErrorController(ErrorAttributes errorAttributes,
      @Value("${debugMode}") boolean debug) {
    super();
    this.errorAttributes = errorAttributes;
    this.debug = debug;
  }

  @Override
  public String getErrorPath() {
    return ERROR_PATH;
  }

  @RequestMapping
  Response<?> error(HttpServletRequest request, HttpServletResponse response) {
    Map<String, Object> data = getErrorAttributes(request, debug);
    String message = (String) data.remove("message");
    return new Response<>(Boolean.FALSE, String.valueOf(response.getStatus()), message, data);
  }

  private Map<String, Object> getErrorAttributes(HttpServletRequest request,
      boolean includeStackTrace) {
    RequestAttributes requestAttributes = new ServletRequestAttributes(request);
    return errorAttributes.getErrorAttributes(requestAttributes, includeStackTrace);
  }

}
