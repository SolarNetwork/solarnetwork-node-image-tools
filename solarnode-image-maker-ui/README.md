# SolarNode Image Maker UI

This project contains a webapp that uses a [SolarNode Image Maker (NIM)][nim] server to
produce a customized SolarNode OS image file.

Here's a demo of how the app works:

![demo](docs/solarssh-demo-shell.gif)

# Use

The app includes built-in instructions on how to use it.

# Building

The build uses [NPM][npm] or [Yarn][yarn]. First, initialize the dependencies:

```shell
# NPM
npm install

# or, Yarn
yarn install
```

Then, the development web server can be started via

```shell
# NPM
npm run start

# or, Yarn
yarn run start
```

and then the app can be reached at [localhost:9000](http://localhost:9000). For a
produciton build, use

```shell
# NPM
npm run build -- --config webpack.prod.js

# or, Yarn
yarn run build --config webpack.prod.js
```

and the app will be built in the `dist` directory.

# Development

This app uses the [SolarNetwork Core library][sn-api-core].

[npm]: https://www.npmjs.com/
[yarn]: https://yarnpkg.com/
[nim]: https://github.com/SolarNetwork/solarnetwork-node-image-tools/tree/master/solarnode-image-maker
[sn-api-core]: https://github.com/SolarNetwork/sn-api-core-js
