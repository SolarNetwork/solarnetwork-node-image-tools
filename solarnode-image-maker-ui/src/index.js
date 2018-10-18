import 'dialog-polyfill/dialog-polyfill.css';
import './nim.css';

import { library, dom } from '@fortawesome/fontawesome-svg-core'
import { faPlusCircle } from '@fortawesome/free-solid-svg-icons'

import startApp from './nim.js';

library.add(faPlusCircle);
dom.watch();

if ( !window.isLoaded ) {
	window.addEventListener("load", function() {
		startApp();
	}, false);
} else {
	startApp();
}
