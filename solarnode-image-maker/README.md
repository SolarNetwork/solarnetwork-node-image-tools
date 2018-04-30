# SolarNode Image Maker

This project provides a RESTful webapp that helps creating customized SolarNode
operating system image files. At a high level, creating a customized image
through this application works like this:

 * the application provides access to a library of _base images_ that serve
   as starting points for customizations
 * creates a copy of a chosen base image
 * applies customizations (provided by you) to the base image copy
 * compresses the customized image
 * allows you to download the compressed customized image

# Customization

Customization works via a restricted version of the [libguestfs][libguestfs]
library's [guestfish][guestfish] scripting language. The following fishscript
commands are not supported:

| Command   | Description                                                                  |
|-----------|------------------------------------------------------------------------------|
| `!`       | Local commands are not allowed.                                              |
| `<!`      | Local command redirection is not allowed.                                    |
| `\|`      | The pipe-to-local-command feature is not supported.                          |
| `display` | Displaying images are not supported.                                         |
| `edit`    | Interactive editing is not supported (`emacs` and `vi` are also restricted). |
| `event`   | Shell events are not supported.                                              |
| `lcd`     | The "local change directory" command is not allowed.                         |

Basically, using local OS commands are not allowed. A typical fishscript for
this app would work by using `tar-in` to unarchive a set of files onto the image
followed by a `zero-free-space` to make the image compress better. For example,
the following script expands an `os-files.txz` archive and then calls
`zero-free-space`, both on the root file system of the image:

```
# Copy base data
tar-in os-files.txz / compress:xz

# Zero free space to make compress of image better
zero-free-space /
```

This script, and the `os-files.txz` referenced by it, would be passed to the app via
a `POST` request to the [/api/v1/images/create/`{baseImageId}`/`{key}`](#rest-api) endpoint
described later in this document.

## First boot scripts

Scripts can be installed that will be executed when the OS boots up for the first time.
The scripts can do whatever you like, such as install packages. Simply name any scripts
you'd like to run at first boot with a `.firstboot` extension and include them when
you [execute the image customization process](#start-custom-image-task). For example:

```
#!/usr/bin/env sh

apt-get -qq update
apt-get -qq install vim-nox
```


# Building

Gradle is used for building. Run the `build` task via `gradlew` to build an executable WAR:

	$ ./gradlew build

The finished executable WAR file will be `build/libs/solarnode-image-maker-X.war` where `X`
is the version number.

You can also build a traditional, non-executable WAR via the `war` task:

	$ ./gradlew war

The finished WAR file will also be `build/libs/solarnode-image-maker-X.war`.

# Running

The application is restricted to running on a host that has
[libguestfs][libguestfs] available. In practical terms that means Linux
at the time of this writing.

## System requirements

In general, the application requires the following items:

 1. Java 8 runtime: on Debian systems this is provided by the
    **openjdk-8-jre-headless** package.
 2. [libguestfs][libguestfs]: on Debian systems this is provided by the
    **libguestfs-tools** package.
 3. XZ compression support: on Debian systems this is provided by the
    **xz-utils** package (needed if any fishscripts need XZ support)
 4. QEMU emulation support: on Debian systems this is provided by the
    **qemu-user-static** package (needed if needing to manipulate
    images for architectures different from the host, like ARM for
    the Raspberry Pi)

## Running standalone

The executable WAR file can be directly executed like this:

	$ java -Dspring.profiles.active=production -jar build/libs/solarnode-image-maker-0.1.war

This will start the web server on port **8080** by default. The REST API can
be accessed, for example, like

	$ curl http://localhost:8080/api/v1/images/infos

The `-Dspring.profiles.active=production` argument specifies the configuration
profile to use, which can be one of `development`, `staging`, or `production`.
To customize the configuration, create an `application-PROFILE.yml`
configuration file in the working directory from which you start the app, where
`PROFILE` is the profile name you're using.

## Running in servlet container

If the WAR is deployed into a servlet container (e.g. Tomcat) then a servlet
context path of `/solarnode-image-maker` is used by default. Assuming the
container is listening on port **8080**, to access the REST API you'd need to
include that context path at the start at each API endpoint, for example:

	$ curl http://localhost:8080/solarnode-image-maker/api/v1/images/infos

To customize the configuration, you can configure servlet environment properties
for the app. The method of doing this is container-specific. For Tomcat, you can
create a `Catalina/localhost/solarnode-image-maker.xml` XML file like this:

```xml
<Context displayName="SolarNode Image Maker">
  <Environment name="spring.profiles.active" value="production" type="java.lang.String" override="false"/>
</Context>
```

## Runtime configuration

The default configuration values can be viewed in the [application.yml][app-config]
source file. The following tables describe the properties in more detail:

### Development runtime configuration

The following settings are applicable only to the `development` runtime profile:

| Setting                        | Default                   | Description                                                                     |
|--------------------------------|---------------------------|---------------------------------------------------------------------------------|
| repo.source.fs.path            | var/repo                  | Path to the image repository used for base image files.                         |
| repo.dest.compression.type     | xz                        | Compression format to use for customized images.                                |
| repo.dest.compression.ratio    | 1                         | Compression level to use for customized images, between 0 (least) and 1 (most). |
| repo.dest.fs.path              | /var/tmp/node-image-repo  | Path to image repository to save customized images to.                          |

### Production runtime configuration

The following settings are applicable to the `staging` and `production` runtime
profiles:

| Setting                        | Default                   | Description                                                                     |
|--------------------------------|---------------------------|---------------------------------------------------------------------------------|
| repo.source.s3.region          | us-west-2                 | S3 region for the image repository used for base image files.                   |
| repo.source.s3.bucket          |                           | S3 bucket for the image repository used for base image files.                   |
| repo.source.s3.objectKeyPrefix | solarnode-images/         | S3 object key prefix for the image repository used for base image files.        |
| repo.source.s3.accessKey       |                           | S3 access key for the image repository used for base image files.               |
| repo.source.s3.secretKey       |                           | S3 secret key for the image repository used for base image files.               |
| repo.source.s3.cache.path      | /var/tmp/node-image-cache | Path to a directory to cache S3 base image files at.                            |
| repo.dest.compression.type     | xz                        | Compression format to use for customized images.                                |
| repo.dest.compression.ratio    | 1                         | Compression level to use for customized images, between 0 (least) and 1 (most). |
| repo.dest.s3.region            | us-west-2                 | S3 region of the image repository to save customized images to.                 |
| repo.dest.s3.bucket            |                           | S3 bucket of the image repository to save customized images to.                 |
| repo.dest.s3.objectKeyPrefix   | solarnode-custom-images/  | S3 object key prefix of the image repository to save customized images to.      |
| repo.dest.s3.accessKey         |                           | S3 access key of the image repository to save customized images to.             |
| repo.dest.s3.secretKey         |                           | S3 secret key of the image repository to save customized images to.             |
| repo.dest.s3.work.path         | java.io.tmpdir            | Path to a directory to use for temporary customized image file data.            |

**Notes:**

 * The `repo.source.s3.accessKey` and `repo.source.s3.secretKey` values are optional
   if the S3 bucket is publicly accessible.
 * If the `repo.dest.s3.region` and `repo.dest.s3.bucket` values match their
   `source` equivalents and `repo.dest.s3.accessKey` is not configured, the source
   image repository will also be used as the destination repository.

### SolarNetwork authorization runtime configuration

The following settings are applicable only to the `snauth` runtime profile. If this profile is activated
at runtime, then the `/authorize` endpoint **must** be called to generate an authorized key. See the
REST API documentation below for more information.

| Setting          | Default                       | Description                       |
|------------------|-------------------------------|-----------------------------------|
| solarnet.baseUrl | https://data.solarnetwork.net | Base URL to the SolarNetwork API. |


# REST API

The REST API is pretty simple:

| Verb | Path                                          | Description                     |
|------|-----------------------------------------------|---------------------------------|
| POST | /api/v1/images/authorize                      | Get an authorized key.          |
| GET  | /api/v1/images/infos                          | List all available base images. |
| POST | /api/v1/images/create/`{baseImageId}`/`{key}` | Start image customization task. |
| GET  | /api/v1/images/receipt/`{receiptId}`/`{key}`  | Get image task info and status. |
| GET  | /api/v1/images/`{receiptId}`/`{key}`          | Get a customized image.         |

The path variables used are:

| Variable      | Description                                                    |
|---------------|----------------------------------------------------------------|
| `baseImageId` | The `id` property of a base image from `/api/v1/images/infos`. |
| `key`         | A unique, random customization key.                            |
| `receiptId`   | The `id` property of a customization task receipt.             |

The customization process thus follows a typical flow:

 1. Call `/api/v1/images/infos` to get the `baseImageId` value of the image you
    want to customize.
 2. Call `/api/v1/images/authorize` to generate a random `key`. See below for more
    information on this endpoint.
 3. Call `/api/v1/images/create/{baseImageId}/{key}` using the `key` returned
    from the previous `/authorize` call, with multi-part attachments for all the
    data files and script you'll use to customize the image. This will return a
    `receiptId`.
 4. Call `/api/v1/images/receipt/{receiptId}/{key}` to check the progress of the
    customization task, and wait until the response `done` property is `true`.
 5. Download the customized image, either from a URL provided by the `downloadUrl`
    property of the receipt, or via a call to `/api/v1/images/{receiptId}/{key}`.

## Authorized keys

The `images/create` endpoint may require authorized keys to function. This is
determined at runtime by activating the `snauth` profile at launch. When active,
you must call the `images/authorize` endpoint with a pre-signed [SNWS2
authorization][snws2] HTTP header value and associated authorization date. The
pre-signed header value must be provided on a `X-SN-PreSignedAuthorization` HTTP
header, and the date on a `X-SN-Date` HTTP header.

The pre-signed URL requirements are:

| Variable  | Description                                                                                   |
|-----------|-----------------------------------------------------------------------------------------------|
| HTTP verb | Must be `GET`.                                                                                |
| URL       | Absolute URL to `/solaruser/api/v1/sec/whoami` endpoint from the `solarnet.baseUrl` base URL. |

An example of this is shown below.


# Example REST API use

Here's a more detailed example of the REST interactions that make up the typical
image customization process, with example request and response values.

## List the available base images

Invoking `GET` on the `/api/v1/images/infos` endpoint will return a list of
available base images to choose from:

```json
{
  "success": true,
  "data": [
    {
      "id": "solarnode-deb8-ebox3300mx-1GB",
      "sha256": "033aa701b80ff9e5037e6d1fe143f3e1564b5e4a815890a097c9ef3ad84e2680",
      "contentLength": 260171204,
      "uncompressedSha256": "f7539a878fdca553dfbeb9dda70835d94f774fe972c01e42145277f77861497d",
      "uncompressedContentLength": 1000341504
    }
  ]
}
```

In this example just one base image is available: `solarnode-deb8-ebox3300mx-1GB`.

## Get authorized key

Typically an authorized key will be required. To get one, we must use
[SolarNetwork token credentials][snws2] to pre-sign a `GET` request to the
`/solaruser/api/v1/sec/whoami` SolarNetwork endpoint. This simply validates
the requestor is a registered SolarNetwork user.

Invoking `POST` on the `/api/v1/images/authorize` endpoint thus requires
passing two HTTP headers:

 1. `X-SN-Date` - the date used to pre-sign the call to the `/whoami` endpoint.
 2. `X-SN-PreSignedAuthorization` - the full pre-signed `Authorization` HTTP
    header value for the `/whoami` endpoint.

For example, for a token `foobar` with a secret `123456` requested on Mon, 30
Oct 2017 04:23:23 GMT, the HTTP request would look similar to this:

```
POST /solarnode-image-maker/api/v1/images/authorize HTTP/1.1
Content-Length: 0
X-SN-Date: Mon, 30 Oct 2017 04:23:23 GMT
X-SN-PreSignedAuthorization: SNWS2 Credential=foobar,SignedHeaders=host;x-sn-date,Signature=c13055afe54c1bff15b8267c11d9755d7217c0318c6c5e1c6b0472cf3cb1e03c
```

**Note** that the `X-SN-Date` HTTP header value used in this request must be the
_same_ as used in the pre-signed authorization header.

The result of invoking this endpoint will be a unique key that must be passed to
subsequent endpoint calls. For example:

```json
{
  "success": true,
  "data": "1f7b52217eb50d64b06e3185cb3bcdeb21de1aa4db42466acc047835c71b1bee"
}
```

## Start custom image task

Customizing an image take several minutes to complete, so it happens via an
asynchronous process. To kick things off, invoke `POST` on the
`/api/v1/images/create/{baseImageId}/{key}` endpoint, which requires the base
image ID and a unique key. The `key` will usually be the response from a
previous call to the `/api/v1/images/authorize` endpoint, as described above. If
authorized keys are not required, the `key` can be anything; a good choice would
be a random UUID. In this example we'll just use `mykey`.

This endpoint accepts `multipart/form-data` content of multiple file attachments.
You can include any number of `dataFile` parts for the custom data you will
be customizing the base image with. At least one `dataFile` part for a `*.fish`
file is required: this is the `guestfish` script that will be Customizing
the image for you. Any number of `*.firstboot` files can be included and they
will be installed into the image so they are executed the first time the OS
boots up.

This endpoint also accepts a `options` file attachment that is a JSON document
with various options that control the customization task.

In the following example, a `os-files.txz` tarball `dataFile` is posted, along
with a `solarnode-deb8-test.fish` script file. An `options` JSON file is
included that turns on verbose messages and sets some environment variables:

```
POST /api/v1/images/create/solarnode-deb8-ebox3300mx-1GB/mykey HTTP/1.1
Content-Type: multipart/form-data; charset=utf-8; boundary=__X_NIM_BOUNDARY__
Content-Length: 913

--__X_NIM_BOUNDARY__
Content-Disposition: form-data; name="dataFile"; filename="os-files.txz"
Content-Type: application/octet-stream

<<raw bytes here>>
--__X_NIM_BOUNDARY__
Content-Disposition: form-data; name="dataFile"; filename="solarnode-deb8-test.fish"
Content-Type: application/octet-stream

# Copy base data
tar-in os-files.txz / compress:xz

# Zero free space to make compress of image better
zero-free-space /

--__X_NIM_BOUNDARY__
Content-Disposition: form-data; name="options"; filename="solarnode-deb8-test-options.json"
Content-Type: application/json

{
	"verbose": true,
	"parameters": {
		"format": "raw"
	},
	"environment": {
		"profile": "development",
		"number": 42
	}
}

--__X_NIM_BOUNDARY__--
```

After all the data is posted, the response includes a _receipt_ that provides
a receipt object with status information and a unique ID for your custom image:

```json
{
  "success": true,
  "data": {
    "createdDate": 1508450247355,
    "id": "1323214e-6abf-4e34-8487-b38f5302e3b2",
    "baseImageId": "solarnode-deb8-ebox3300mx-1GB",
    "percentComplete": 0.0,
    "cancelled": false,
    "done": false,
    "started": true,
    "startedDate": 1508450247376,
    "message": "Uncompressing source image"
  }
}
```

The application will have added the image customization task to a queue for
asynchronous processing. Once work begins on the task, the `started` property
will change to `true` and the `percentComplete` will increase until the work is
finished. Once finished the `done` property will change to `true`.

## Check status

Because creating the custom image can take a while, invoking `GET` on the
`/api/v1/images/receipt/{receiptId}/{key}` endpoint will provide a receipt
object with status information about the progress of the task. The `receiptId`
value comes from the previous call to
`/api/v1/images/create/{baseImageId}/{key}` and `key` is also the same value
from that call. An example response looks like this:

```json
{
  "success": true,
  "data": {
    "createdDate": 1508454129211,
    "id": "03920daf-f0cc-4059-aa87-7192ddf82176",
    "baseImageId": "solarnode-deb8-ebox3300mx-1GB",
    "percentComplete": 0.24285492007337528,
    "cancelled": false,
    "done": false,
    "started": true,
    "startedDate": 1508454129238,
    "message": "Uncompressing source image"
  }
}
```

The `percentComplete` will show how much work has been completed and `message`
will contain information on what work is being performed. If an error occurs
`message` will include an error message.

Once the task completes, the `done` value will change to `true` and if there
where no errors, the response will include more details about the customized
image in an `imageInfo` property. For example:

```json
{
  "success": true,
  "data": {
    "createdDate": 1508521059920,
    "id": "337a4031-b600-4c1c-9181-fc46fa9da630",
    "baseImageId": "solarnode-deb8-ebox3300mx-1GB",
    "startedDate": 1508521059920,
    "completedDate": 1508521532152,
    "imageInfo": {
      "filename": "1cadc649-881b-45ee-85af-42a84b95993d.img.xz",
      "uncompressedContentLength": 1000341504,
      "uncompressedSha256": "b38ecb3c2d67c7c65196346f7190e74c424c40ffcda9443d86cedea2f3a1fddb",
      "sha256": "9545a2ffef191432d877977c3cacd143ead88b1b719cb0fd7a76dcac714f76b4",
      "contentLength": 290128104,
      "id": "1cadc649-881b-45ee-85af-42a84b95993d"
    },
    "downloadUrl": "https://testing.s3-us-west-2.amazonaws.com/solarnode-custom-images/node-image-data/1f6fea5a-7466-48f3-aa3a-1366009be69f.img.xz?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20171025T230900Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=AKIAID...%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Signature=f996dd2be40...",
    "percentComplete": 1.0,
    "done": true,
    "cancelled": false,
    "started": true,
    "message": "Done"
  }
}
```

## Download customized image

Once the customization task completes, you can download the image either via the
`downloadUrl` property returned in the status info of the
`/api/v1/images/receipt/{receiptId}/{key}` endpoint or by calling the
`/api/v1/images/{receiptId}/{key}` endpoint. Generally you should use the
`downloadUrl` if it is provided. In this example, a pre-signed S3 URL was
provided so the image can be downloaded directly from S3.


 [app-config]: https://github.com/SolarNetwork/solarnetwork-node-image-tools/blob/master/solarnode-image-maker/src/main/resources/application.yml
 [guestfish]: http://libguestfs.org/guestfish.1.html
 [libguestfs]: http://libguestfs.org/
 [snws2]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-authentication-scheme-V2
