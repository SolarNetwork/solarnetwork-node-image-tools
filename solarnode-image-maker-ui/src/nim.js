/* eslint-env es6, browser, commonjs */
"use strict";

import {
  AuthorizationV2Builder,
  Configuration,
  urlQuery,
  UrlHelper,
  UserUrlHelperMixin
} from "solarnetwork-api-core";
import {
  NimUrlHelper,
  SolarNodeImageGroup,
  SolarNodeImageInfo,
  SolarNodeImageReceipt
} from "solarnetwork-api-nim";
import { event as d3event, select, selectAll } from "d3-selection";
import { json as jsonRequest } from "d3-request";
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

/** The HTML data property name used to hold a `SolarNodeImageReceipt` ID. */
const DATA_RECEIPT_ID = "receiptId";

/** The default receipt refresh rate, in milliseconds. */
const DEFAULT_REFRESH_RECEIPT_RATE = 5000;

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
  const self = { version: "0.1.0" };
  const config = options || {
    solarNetworkAuthorization: true,
    receiptRefreshRate: DEFAULT_REFRESH_RECEIPT_RATE
  };

  /** @type {SolarNodeImageInfo[]} */
  var images = [];

  /** @type {SolarNodeImageGroup[]} */
  var imageGroups = [];

  /** @type {SolarNodeImageInfo} */
  var activeImage = undefined;

  /** @type {SolarNodeImageReceipt[]} */
  var receipts = [];

  /**
   * Get/set the receipt refresh rate.
   *
   * @param {number} [val] the rate to set, in milliseconds
   * @returns {number|this} when used as a getter, the current rate; when used as a setter this object
   */
  function receiptRefreshRate(val) {
    if (!val) {
      let v = config.receiptRefreshRate;
      if (!v) {
        v = DEFAULT_REFRESH_RECEIPT_RATE;
      }
      return v;
    }
    config.receiptRefreshRate = v;
    return self;
  }

  function toggleAlert(enabled, message) {
    select("#alert")
      .classed("hidden", !enabled)
      .select(".message")
      .text(message || "");
  }

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
    const authBtn = select("#authorize");
    authBtn.attr("disabled", "disabled");
    const tokenId = select("input[name=token]").property("value");
    const authBuilder = new AuthorizationV2Builder(tokenId, snUrlHelper);
    authBuilder.saveSigningKey(select("input[name=secret]").property("value"));

    if (config.solarNetworkAuthorization == "true") {
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
      authBtn.attr("disabled", null);
      if (!(json && json.success && json.data)) {
        console.error("Failed to authorize session: %s", JSON.stringify(json));
        return;
      }
      console.info("Got image authorization session key: %s", json.data);
      toggleResultHidden("auth", true);

      selectAll(".hide-after-auth").classed("hidden", true);

      // stash session key onto the NIM UrlHelper
      nimUrlHelper.nimSessionKey = json.data;

      // reload image list, in case auth was required
      listBaseImages();
    }

    function authorizeError(event) {
      const xhr = event.target;
      authBtn.attr("disabled", null);
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
      images = json.data.map(d => SolarNodeImageInfo.fromJsonEncoding(d));
      images.sort(SolarNodeImageInfo.compareById);

      imageGroups = SolarNodeImageInfo.idComponentGroups(images);
      renderImageGroups();
    }

    function listBaseImagesError(event) {
      const xhr = event.target;
      console.error("Failed to list base images: %s %s", xhr.status, xhr.responseText);
    }
  }

  /**
   * Find an available base image based on its ID value.
   *
   * This searches the images previously discovered via `listBaseImages()`.
   *
   * @param {string} id the image ID to look for
   * @return {SolarNodeImageInfo} the found image, or `undefined` if not found
   */
  function baseImageForId(id) {
    return images.find(function(d) {
      return d.hasId(id);
    });
  }

  /**
   * Select an image to work with.
   *
   * @param {SolarNodeImageInfo} image the image to work with
   */
  function chooseBaseImage(image) {
    console.info("Selected base image %s", image.id);
    activeImage = image;
    selectAll(".active-image-name").text(image.id);
    select("section.prepare").classed("disabled", false);
    selectAll("section.prepare input[disabled]").attr("disabled", null);
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
    select("#base-image-listing").classed("hidden", false);
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

          // note we insert in reverse order, assuming the component name is a date so most recent first
          itemsEl.insertBefore(itemEl, itemsEl.firstChild);
        });

        groupEl.appendChild(itemsEl);
      }

      // add group to parent
      parent.appendChild(groupEl);
    }
  }

  /**
   * Schedule a repeating task to refresh the receipt status of a given receipt.
   *
   * @param {SolarNodeImageReceipt} receipt the receipt to refresh
   */
  function scheduleReceiptRefresh(receipt) {
    setTimeout(function() {
      const url = nimUrlHelper.createImageReceiptUrl(receipt.id);
      const req = jsonRequest(url)
        .on("load", function(json) {
          if (json && json.success && json.data) {
            const idx = receipts.indexOf(receipt);
            const newReceipt = SolarNodeImageReceipt.fromJsonEncoding(json.data);
            receipts[idx] = newReceipt;
            renderReceipt(newReceipt);
            if (!newReceipt.done) {
              scheduleReceiptRefresh(newReceipt);
            }
          } else {
            console.error("Error getting receipt %s: %s", receipt.id, JSON.serialize(json));
          }
        })
        .on("error", function(event) {
          const xhr = event.target;
          console.error("Failed to authorize session: %s %s", xhr.status, xhr.responseText);
        });
      req.send("GET");
    }, receiptRefreshRate());
  }

  function renderReceipts() {
    receipts.forEach(renderReceipt);
  }

  /**
   * Render a receipt as HTML.
   *
   * @param {SolarNodeImageReceipt} receipt the receipt to render
   */
  function renderReceipt(receipt) {
    const containerEl = document.getElementById("progress-container");
    const kids = containerEl.children;
    var receiptEl;
    for (let i = 0; i < kids.length; i += 1) {
      receiptEl = kids[i];
      let receiptId = receiptEl.dataset[DATA_RECEIPT_ID];
      if (receipt.id === receiptId) {
        renderReceiptUpdate(receiptEl, receipt);
        return;
      }
    }
    // not found; add new
    const template = document.getElementById("image-receipt-template");
    receiptEl = template.cloneNode(true);
    receiptEl.removeAttribute("id");
    receiptEl.classList.remove("template");
    receiptEl.dataset[DATA_RECEIPT_ID] = receipt.id;
    renderReceiptUpdate(receiptEl, receipt);
    containerEl.appendChild(receiptEl);
  }

  /**
   * Render a receipt into HTML.
   *
   * @param {Element} receiptEl the receipt container
   * @param {SolarNodeImageReceipt} receipt the receipt to render
   */
  function renderReceiptUpdate(receiptEl, receipt) {
    select(receiptEl)
      .selectAll(".base-image-id")
      .text(receipt.baseImageId);

    select(receiptEl)
      .selectAll(".receipt-message")
      .text(receipt.message);

    const progress = receipt.percentComplete * 100;

    select(receiptEl)
      .selectAll(".progress")
      .style("width", `${progress}%`)
      .text(`${Math.round(progress)}%`);

    if (receipt.done) {
      let url = receipt.downloadUrl;
      if (!url) {
        url = nimUrlHelper.downloadImageUrl(receipt.id);
      }
      select(receiptEl)
        .select(".download-image")
        .attr("href", url)
        .classed("disabled", false);
    }
  }

  function updateProgressBarProgress(progressBarSelection, percent) {
    const percentComplete = percent * 100;
    progressBarSelection
      .style("width", `${percentComplete}%`)
      .text(`${Math.round(percentComplete)}%`);
  }

  function submit() {
    const imageId = activeImage ? activeImage.id : undefined;
    if (!imageId) {
      console.error("No base image selected, cannot submit.");
    }
    const submitBtn = select("#submit-btn");
    submitBtn.attr("disabled", "disabled");

    // submit multipart form
    const form = document.getElementById("nim-form");
    const xhr = new XMLHttpRequest();
    const url = nimUrlHelper.createImageUrl(imageId);
    xhr.onload = submitSuccess;
    xhr.onerror = submitError;
    xhr.upload.addEventListener("progress", submitProgress);
    xhr.open("POST", url);
    xhr.setRequestHeader("Accept", "application/json");
    xhr.send(new FormData(form));

    const uploadProgressContainer = select("#submit-progress");
    const uploadProgressBar = uploadProgressContainer.select(".progress");
    updateProgressBarProgress(uploadProgressBar, 0);
    uploadProgressContainer.classed("hidden", false);

    function submitSuccess() {
      uploadProgressContainer.classed("hidden", true);
      submitBtn.attr("disabled", null);

      var json = undefined;
      console.info("Submitted image, got response: %s", xhr.responseText);
      if (xhr.responseType === "json") {
        json = xhr.response;
      } else {
        json = JSON.parse(xhr.responseText);
      }
      if (json && json.success && json.data) {
        let receipt = SolarNodeImageReceipt.fromJsonEncoding(json.data);
        receipts.push(receipt);
        renderReceipts();
        scheduleReceiptRefresh(receipt);
      }
    }

    function submitProgress(event) {
      if (event.lengthComputable) {
        const percentComplete = event.loaded / event.total;
        console.info("Upload progress: %d%%", percentComplete * 100);
        updateProgressBarProgress(uploadProgressBar, percentComplete);
      }
    }

    function submitError() {
      uploadProgressContainer.classed("hidden", true);
      submitBtn.attr("disabled", null);
      console.error(
        "Error submitting image (%s), got response: %s",
        xhr.statusText,
        xhr.responseText
      );
      toggleAlert(
        true,
        `Error submitting image ${imageId}: ${xhr.statusText}: ${xhr.responseText}`
      );
    }
  }

  function start() {
    // nothing to do
    return this;
  }

  function stop() {
    // nothing to do
    return this;
  }

  function handleBaseImageListClick() {
    const event = d3event;
    var target = event.target;
    while (target && target.nodeName !== "LI") {
      target = target.parentNode;
    }

    if (target && target.classList.contains("item")) {
      const imageId = target.dataset[DATA_IMAGE_ID];
      if (imageId) {
        const image = baseImageForId(imageId);
        if (image) {
          chooseBaseImage(image);
        }
      }
    }
  }

  function handleAuthorizationInputKeyup() {
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
  }

  function handleAddFirstbootClick() {
    const addBtn = document.getElementById("add-firstboot");
    const container = document.getElementById("firstboot-container");
    const newInput = document.createElement("input");
    newInput.setAttribute("type", "file");
    newInput.setAttribute("name", "dataFile");
    newInput.setAttribute("accept", "*.firstboot");
    container.insertBefore(document.createElement("br"), addBtn);
    container.insertBefore(newInput, addBtn);
  }

  function handleAddDataFileClick() {
    const addBtn = document.getElementById("add-data-file");
    const container = document.getElementById("data-file-container");
    const newInput = document.createElement("input");
    newInput.setAttribute("type", "file");
    newInput.setAttribute("name", "dataFile");
    newInput.setAttribute("accept", "*.*");
    container.insertBefore(document.createElement("br"), addBtn);
    container.insertBefore(newInput, addBtn);
  }

  function handleSubmitButtonClick() {
    submit();
  }

  function handleFishscriptInputChange() {
    const event = d3event;
    const input = event.target;
    console.log("Selected fishscript: %s", input.value);
    select("section.submit").classed("disabled", false);
    selectAll("#submit-btn").attr("disabled", null);
  }

  function init() {
    select("#authorize").on("click", authorize);
    selectAll("input.auth").on("keyup", handleAuthorizationInputKeyup);
    select("#add-firstboot").on("click", handleAddFirstbootClick);
    select("#add-data-file").on("click", handleAddDataFileClick);
    select("#submit-btn").on("click", handleSubmitButtonClick);
    select("#fishscript").on("change", handleFishscriptInputChange);
    return Object.defineProperties(self, {
      start: { value: start },
      stop: { value: stop }
    });
  }

  return init();
};

export default function startApp() {
  var config = new Configuration(
    Object.assign(
      {
        solarNetworkAuthorization: true,
        receiptRefreshRate: DEFAULT_REFRESH_RECEIPT_RATE
      },
      urlQuery.urlQueryParse(window.location.search)
    )
  );

  var snUrlHelper = new UserUrlHelper(snEnv);

  var nimUrlHelper = new NimUrlHelper(nimEnv);

  app = nimApp(nimUrlHelper, snUrlHelper, config).start();

  window.onbeforeunload = function() {
    app.stop();
  };

  return app;
}
