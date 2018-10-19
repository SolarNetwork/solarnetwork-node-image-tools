import "dialog-polyfill/dialog-polyfill.css";
import "./nim.css";

import { library, dom } from "@fortawesome/fontawesome-svg-core";
import {
  faCheckCircle,
  faHdd,
  faMinusSquare,
  faPlusCircle,
  faPlusSquare,
  faSync,
  faTimesCircle
} from "@fortawesome/free-solid-svg-icons";

import startApp from "./nim.js";

library.add(faCheckCircle, faHdd, faMinusSquare, faPlusCircle, faPlusSquare, faSync, faTimesCircle);
dom.watch();

if (!window.isLoaded) {
  window.addEventListener(
    "load",
    function() {
      startApp();
    },
    false
  );
} else {
  startApp();
}
