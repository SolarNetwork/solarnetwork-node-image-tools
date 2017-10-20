# SolarNode Image Maker

This project provides a RESTful webapp that helps creating customized SolarNode
operating system image files. At a high level, the creating a customized image
through this application works like this:

 * the application provides access to a library of _base images_ that serve
   as starting points for customizations
 * creates a copy of a chosen base image
 * applies customizations (provided by you) to the base image copy
 * compresses the customized image
 * allows you to download the compressed customized image

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
be customizing the base image with. At least one `dtaFile` part for a `*.fish`
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
    "created": 1508450247355,
    "id": "1323214e-6abf-4e34-8487-b38f5302e3b2",
    "percentComplete": 0.0,
    "cancelled": false,
    "done": false,
    "started": true,
    "message": "Uncompressing source image"
  }
}
```

The application will have add the image customization task to a queue for
asynchronous processing.

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
    "created": 1508454129211,
    "id": "03920daf-f0cc-4059-aa87-7192ddf82176",
    "percentComplete": 0.24285492007337528,
    "cancelled": false,
    "done": false,
    "started": true,
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
    "created": 1508455801860,
    "id": "87fb8f6c-92dc-4304-8e3f-09817f345d21",
    "percentComplete": 1.0,
    "imageInfo": {
      "filename": "519d6bdfd194b6db6144fccca69bdf3749bf8e3f4e441eb4e1c45dc81c6dfdfc.img.xz",
      "uncompressedContentLength": 1000341504,
      "sha256": "2db6af2d9eff7c69919ce4966bad6cc009c5643dcedda4b7ee36105217fe329d",
      "uncompressedSha256": "348618bd969627068d543287e3f8aa3dd9a286088d9d64fd8d1e790f6baef010",
      "contentLength": 260473232,
      "id": "519d6bdfd194b6db6144fccca69bdf3749bf8e3f4e441eb4e1c45dc81c6dfdfc"
    },
    "cancelled": false,
    "done": true,
    "started": true,
    "message": "Done"
  }
}
```
