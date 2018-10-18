/* eslint-env es6, browser, commonjs */
"use strict";

import {
  AuthorizationV2Builder,
  Configuration,
  urlQuery,
  UrlHelper,
  UserUrlHelperMixin
} from "solarnetwork-api-core";
import { NimUrlHelper, SolarNodeImageGroup, SolarNodeImageInfo } from "solarnetwork-api-nim";
import { event as d3event, select, selectAll } from "d3-selection";
import { json as jsonRequest } from "d3-request";
import dialogPolyfill from "dialog-polyfill";

// for development, can un-comment out the nimEnv and instrEnv objects
// and configure values for your local dev environment.

const nimEnv = null; /*new Environment({
	debug: true,
	protocol: 'http',
	host: 'solarnetworkdev.net',
	port: 8080,
});*/

const snEnv = null; /*new Environment({
	protocol: 'http',
	host: 'solarnetworkdev.net',
	port: 8680,
});*/

var app;

class UserUrlHelper extends UserUrlHelperMixin(UrlHelper) {}

function executeWithSignedAuthorization(method, url, auth) {
  auth
    .snDate(true)
    .date(new Date())
    .method(method)
    .url(url);
  var req = jsonRequest(url);
  req.on("beforesend", function(request) {
    request.setRequestHeader("X-SN-Date", auth.requestDateHeaderValue);
    request.setRequestHeader("Authorization", auth.buildWithSavedKey());
  });
  console.log("Requesting %s %s", method, url);
  req.send(method);
  return req;
}

function executeWithPreSignedAuthorization(method, url, auth, signMethod, signUrl) {
  auth
    .snDate(true)
    .date(new Date())
    .method(signMethod)
    .url(signUrl);
  var req = jsonRequest(url);
  req.on("beforesend", function(request) {
    request.setRequestHeader("X-SN-Date", auth.requestDateHeaderValue);
    request.setRequestHeader("X-SN-PreSignedAuthorization", auth.buildWithSavedKey());
  });
  console.log("Requesting %s %s", method, url);
  req.send(method);
  return req;
}

/**
 * NIM web app.
 *
 * @class
 * @param {NimUrlHelper} nimUrlHelper the URL helper with the `NimUrlHelperMixin` for accessing NIM with
 * @param {UserUrlHelper} [snUrlHelper] the URL helper with the `UserUrlHelperMixin` for accessing SolarNetwork with
 * @param {Object} [options] optional configuration options
 * @param {boolean} [options.solarNetworkAuthorization] `true` to authorize via SolarNetwork, `false` to authorize via NIM directly
 */
var nimApp = function(nimUrlHelper, snUrlHelper, options) {
  var self = { version: "0.1.0" };
  var config = options || {
    solarNetworkAuthorization: false
  };

  /**
   * Toggle a `hidden` class on a set of elements who have either a `success` or `error` class to match the result of performing some action.
   *
   * @param {string} action the name of the action that was performed, which must match the CSS class name to manipulate
   * @param {boolean} success `true` if the result was successful
   */
  function toggleResultHidden(action, success) {
    const sel = `.${action}.result`;
    selectAll(sel).classed("hidden", function() {
      return !this.classList.contains(success ? "success" : "error");
    });
  }

  function authorize() {
    const tokenId = select("input[name=token]").property("value");
    const authBuilder = new AuthorizationV2Builder(tokenId, snUrlHelper);
    authBuilder.saveSigningKey(select("input[name=secret]").property("value"));

    if (config.solarNetworkAuthorization) {
      // get authorized session key from SN
      executeWithSignedAuthorization("GET", snUrlHelper.nimAuthorizeUrl(), authBuilder)
        .on("load", authorizeSuccess)
        .on("error", authorizeError);
    } else {
      // get authorized session key from NIM, passing a pre-signed SN URL to the /whoami endpoint
      executeWithPreSignedAuthorization(
        "POST",
        nimUrlHelper.authorizeImageSessionUrl(),
        authBuilder,
        "GET",
        snUrlHelper.whoamiUrl()
      )
        .on("load", authorizeSuccess)
        .on("error", authorizeError);
    }

    function authorizeSuccess(json) {
      if (!(json.success && json.data)) {
        console.error("Failed to authorize session: %s", JSON.stringify(json));
        return;
      }
      console.info("Got image authorization session key: %s", json.data);
      toggleResultHidden("auth", true);

      // stash session key onto the NIM UrlHelper
      nimUrlHelper.nimSessionKey = json.data;
    }

    function authorizeError(xhr) {
      if (xhr.target) {
        xhr = xhr.target;
      }
      console.error("Failed to authorize session: %s %s", xhr.status, xhr.responseText);
      toggleResultHidden("auth", false);
    }
  }

  function start() {
    // TODO
    return this;
  }

  function stop() {
    // TODO
    return this;
  }

  function init() {
    select("#authorize").on("click", authorize);
    selectAll("input.auth").on("keyup", function() {
      const event = d3event;
      if (event.defaultPrevented) {
        return;
      }
      switch (event.key) {
        case "Enter":
          authorize();
          break;

        default:
          return;
      }
    });
    return Object.defineProperties(self, {
      // property getter/setter functions

      // TODO

      // action methods

      start: { value: start },
      stop: { value: stop }
    });
  }

  return init();
};

export default function startApp() {
  var config = new Configuration(
    Object.assign({ nodeId: 251 }, urlQuery.urlQueryParse(window.location.search))
  );

  var snUrlHelper = new UserUrlHelper(snEnv);

  var nimUrlHelper = new NimUrlHelper(nimEnv);

  //var sshCredDialog = document.getElementById('ssh-credentials-dialog');
  //dialogPolyfill.registerDialog(sshCredDialog);

  app = nimApp(nimUrlHelper, snUrlHelper, config).start();

  window.onbeforeunload = function() {
    app.stop();
  };

  return app;
}
