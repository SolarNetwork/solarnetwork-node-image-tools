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
import { select, selectAll } from "d3-selection";
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

function executeWithPreSignedAuthorization(method, url, auth) {
  auth.snDate(true).date(new Date());
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
 */
var nimApp = function(nimUrlHelper, snUrlHelper, options) {
  var self = { version: "0.1.0" };
  var config = options || {};

  function authorize() {
    // if snUrlHelper is available, authorize via there, so that NIM wakes up if it is sleeping
    if (snUrlHelper) {
      const authUrl = snUrlHelper.nimAuthorizeUrl();
      const tokenId = select("input[name=token]").property("value");
      const authBuilder = new AuthorizationV2Builder(tokenId, snUrlHelper);
      authBuilder.saveSigningKey(select("input[name=secret]").property("value"));
      executeWithSignedAuthorization("GET", authUrl, authBuilder)
        .on("load", json => {
          if (!(json.success && json.data)) {
            console.error("Failed to authorize session: %s", JSON.stringify(json));
            return;
          }
        })
        .on("error", function(xhr) {
          if (xhr.target) {
            xhr = xhr.target;
          }
          console.error("Failed to authorize session: %s %s", xhr.status, xhr.responseText);
        });
    } else {
      // TODO
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
    select("#connect").on("click", authorize);
    select("#end").on("click", stop);
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

function setupUI(env) {
  // TODO
}

export default function startApp() {
  var config = new Configuration(
    Object.assign({ nodeId: 251 }, urlQuery.urlQueryParse(window.location.search))
  );

  setupUI(config);

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
