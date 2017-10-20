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
library's [guestfish][guestfish] scripting language.

TODO

# Example

Here's an example of the REST interactions that make up the typical image
customization process.

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

## Start custom image task

Customizing an image take several minutes to complete, so it happens via an
asynchronous process. To kick things off, invoke `POST` on the
`/api/v1/images/create/{baseImageId}/{key}` endpoint, which requires the base
image ID and a unique key of your choosing for path variables. The `key` can be
anything; a good choice would be a random UUID. In this example we'll just use
`mykey`.

This endpoint accepts `multipart/form-data` content of multiple file attachments.
You can include any number of `dataFile` parts for the custom data you will
be customizing the base image with. At least one `dataFile` part for a `*.fish`
file is required: this is the `guestfish` script that will be Customizing
the image for you.

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
status information and a unique ID for your custom image:

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
`/api/v1/images/receipt/{receiptId}/{key}` endpoint will provide status
information about the progress of the task. The `receiptId` value comes from the
previous call to `/api/v1/images/create/{baseImageId}/{key}` and `key` is also
the same value from that call. An example response looks like this:

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
    "percentComplete": 1.0,
    "done": true,
    "cancelled": false,
    "started": true,
    "message": "Done"
  }
}
```

 [libguestfs]: http://libguestfs.org/
 [guestfish]: http://libguestfs.org/guestfish.1.html
