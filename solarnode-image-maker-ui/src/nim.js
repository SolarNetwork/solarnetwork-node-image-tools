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
import CollapsibleLists from "./collapsible-lists.js";

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

/** The HTML data property name used to hold a `SolarNodeImageInfo` ID. */
const DATA_IMAGE_ID = "imageId";

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

  /** @type {SolarNodeImageGroup[]} */
  var imageGroups = [];

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
      listBaseImages();
    }

    function authorizeError(event) {
      const xhr = event.target;
      console.error("Failed to authorize session: %s %s", xhr.status, xhr.responseText);
      toggleResultHidden("auth", false);
    }
  }

  function listBaseImages() {
    const url = nimUrlHelper.listBaseImagesUrl();
    var req = jsonRequest(url)
      .on("load", listBaseImagesSuccess)
      .on("error", listBaseImagesError);
    req.send("GET");

    function listBaseImagesSuccess(json) {
      if (!(json.success && Array.isArray(json.data))) {
        console.error("Failed to list base images: %s", JSON.stringify(json));
        return;
      }
      const images = json.data.map(d => SolarNodeImageInfo.fromJsonEncoding(d));
      imageGroups = SolarNodeImageInfo.idComponentGroups(images);
      renderImageGroups();
    }

    function listBaseImagesError(event) {
      const xhr = event.target;
      console.error("Failed to list base images: %s %s", xhr.status, xhr.responseText);
    }
  }

  function handleBaseImageListClick() {
    const event = d3event;
    const target = event.target;
    while (target && target.nodeName !== "LI") {
      target = target.parentNode;
    }

    if (target && target.classList.contains("item")) {
      const imageId = target.dataset[DATA_IMAGE_ID];
      if (imageId) {
        console.info("Click on base image %s", imageId);
      }
    }
  }

  function displayNameForGroupedImageInfo(imageInfo) {
    const id = imageInfo.id;
  }

  function renderImageGroups() {
    if (!Array.isArray(imageGroups)) {
      return;
    }
    const root = document.createElement("ul");
    root.id = "base-image-list";
    root.classList.add("collapsibleList", "fa-ul");
    imageGroups.forEach(function(group) {
      appendImageGroup(group, root);
    });
    selectAll("#base-image-listing > ul").remove();

    const container = document.getElementById("base-image-listing");
    container.classList.add("base-item-list");
    container.appendChild(root);
    CollapsibleLists.apply(false, container);
    select(root).on("click", handleBaseImageListClick);

    /**
     * Append an image group to a node.
     *
     * @param {SolarNodeImageGroup} group the group to append
     * @param {Node} parent the parent node to append the group to
     */
    function appendImageGroup(group, parent) {
      const groupEl = document.createElement("li");

      // create open/closed icons
      const closedListIcon = document.createElement("span");
      closedListIcon.classList.add("fa-li", "closed");
      const closedIcon = document.createElement("i");
      closedIcon.classList.add("fas", "fa-plus-square");
      closedListIcon.appendChild(closedIcon);
      groupEl.appendChild(closedListIcon);

      const openListIcon = document.createElement("span");
      openListIcon.classList.add("fa-li", "open");
      const openIcon = document.createElement("i");
      openIcon.classList.add("fas", "fa-minus-square");
      openListIcon.appendChild(openIcon);
      groupEl.appendChild(openListIcon);

      // create group name
      groupEl.appendChild(document.createTextNode(group.componentName));

      // recurse into child groups
      if (Array.isArray(group.groups)) {
        group.groups.forEach(function(childGroup) {
          const childRoot = document.createElement("ul");
          childRoot.classList.add("fa-ul");
          appendImageGroup(childGroup, childRoot);
          groupEl.appendChild(childRoot);
        });
      }

      // add items
      if (Array.isArray(group.items) && group.items.length > 0) {
        const itemsEl = document.createElement("ul");
        itemsEl.classList.add("fa-ul");

        group.items.forEach(function(item) {
          const itemEl = document.createElement("li");
          itemEl.classList.add("item");

          // stash the image ID as data on the element
          itemEl.dataset[DATA_IMAGE_ID] = item.id;

          // create item icon
          const itemListIcon = document.createElement("span");
          itemListIcon.classList.add("fa-li");
          const itemIcon = document.createElement("i");
          itemIcon.classList.add("fas", "fa-hdd");
          itemListIcon.appendChild(itemIcon);
          itemEl.appendChild(itemListIcon);

          // create item name
          itemEl.appendChild(document.createTextNode(item.displayNameForComponent()));

          itemsEl.appendChild(itemEl);
        });

        groupEl.appendChild(itemsEl);
      }

      // add group to parent
      parent.appendChild(groupEl);
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
      refreshBaseImageList: { value: listBaseImages },

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
  app.refreshBaseImageList();

  window.onbeforeunload = function() {
    app.stop();
  };

  return app;
}
