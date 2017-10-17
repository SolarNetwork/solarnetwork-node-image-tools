/* ==================================================================
 * PingController.java - 18/10/2017 7:22:26 AM
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import net.solarnetwork.web.domain.Response;

/**
 * Web controller for health check type requests.
 * 
 * @author matt
 * @version 1.0
 */
@RestController
@RequestMapping(path = "/api/v1", method = RequestMethod.GET)
public class PingController {

  @RequestMapping("/ping")
  @ApiOperation(value = "", notes = "Validate the server's health. The health can be "
      + "considered good if the response data contains an `allGood` key with " + "a `true` value.")
  public Response<Map<String, ?>> ping() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("allGood", true);
    return Response.response(data);
  }

}
