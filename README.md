<table class="table" style="margin-top: 10px">
    <thead>
    <tr>
        <th>Title</th>
        <th>Last Updated</th>
        <th>Summary</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>Http service</td>
        <td>January 3, 2024</td>
        <td>Detailed description of the API of the Http service.</td>
    </tr>
    </tbody>
</table>

# Overview

The HTTP service allows making HTTP requests as well as receiving HTTP requests from
other servers. This is the list of features:

- Send HTTP requests (`GET`, `PUT`, `POST`, etc.) to other servers
- Receive external HTTP requests (`GET`, `PUT`, `POST`, etc.)
- Download files
- Support for basic and digest authentication
- Support for Cookies, Headers and Query parameters
- Support for JSON and XML content types
- Support for redirects
- Support for SSL

In many cases, you can use the HTTP service to call external services where an official
service does not exist. This will work as long as they provide a REST HTTP API.

## Configuration

### Remember cookies

Enable this flag if you want to use a basic system to exchange cookies with the external 
service, where the service will send the last received cookies in subsequent requests.
Default value: true

### Allow External URLs

Disable this flag if you want to restrict the service to only allow requests with the same domain as the service. 
This is useful to avoid external requests to other services. This will ignore the `Base URL` configuration.
Default value: true

### Connection timeout

This is the maximum time the service waits to perform the connection to an external 
service.
If it times out, an exception is thrown and the request canceled. 
Default value: 5000 ms (5 sec).
Set to zero for to wait indefinitely. 

### Read timeout

This is the maximum time the service waits to receive the response to a request to 
an external service. If it times out, an exception is thrown and the request 
canceled. Default value: 60000 ms (60 sec). Set to zero to wait indefinitely.

### Follow redirects

If it is enabled, the service will automatically redirect when it receives a 
`3xx` HTTP status code as response to a request.
Default value: true

### Webhook URL

This is the URL the service will be listening for requests, which will be sent as events
to the app.

### Sync Webhook URL

This is the URL the service will be listening for requests, which will be sent as events
to the app.

The difference with the webhooks above is that in this case, the listener should return a
JSON object that will be returned to the caller.

# Quick start

You can make a simple `GET` request like this:

```js
var res = svc.http.get({
  url: 'https://postman-echo.com/get',
  params: {
    foo1: '1'
  }
});
log('Response: '+ JSON.stringify(res));
```

Also, a `POST` request can send information like this:

```js
var res = svc.http.post({
  url: 'https://postman-echo.com/post',
  body: {
    name: 'test1'
  }
});
log('Response: '+ JSON.stringify(res));
```

If the response code is not `2XX` you can catch the exception:

```js
try {
  let responseError = svc.http.get({url: "https://mock.codes/403"});
} catch (e) {
  log("Full error: " + JSON.stringify(e));
  log("Short error description: " + JSON.stringify((e.message)));

  log("Internal error: " + JSON.stringify((e.additionalInfo.error)));
  log("Error description: " + JSON.stringify((e.additionalInfo.description)));

  log("Original request: " + JSON.stringify(e.additionalInfo.request));
  log("Timestamp: " + JSON.stringify((e.additionalInfo.details.date)));
  log("Status code: " + JSON.stringify((e.additionalInfo.details.data.additionalInfo.status)));
  log("Body: " + JSON.stringify((e.additionalInfo.details.data.additionalInfo.body)));
  log("Headers: " + JSON.stringify((e.additionalInfo.details.data.additionalInfo.headers)));
}
```

In webhooks, you will have all the information of the request in the event:

```js
sys.logs.info('request info: ' + JSON.stringify(event.data.requestInfo));
sys.logs.info('request headers: ' + JSON.stringify(event.data.headers));
sys.logs.info('request body: ' + JSON.stringify(event.data.body));
```
## Authentication

### Basic

```js
var res = svc.http.get({
  url: 'https://postman-echo.com/basic-auth',
  params: {
    foo1: '1'
  },
  authorization : {
    type: "basic",
    username: "postman",
    password: "password"
  }
});
log('Response: '+ JSON.stringify(res));
```

### Digest

```js
var res = svc.http.get({
  url: 'https://postman-echo.com/digest-auth',
  params: {
    foo1: '1'
  },
  authorization : {
    type: "digest",
    username: "postman",
    password: "password"
  }
});
log('Response: '+ JSON.stringify(res));
```

Any other unlisted authentication method can use the header to send the required data, for example, a token.

```js
var res = svc.http.get({
  url: 'https://postman-echo.com/headers',
  params: {
    foo1: '1'
  },
  headers: {
    'Content-Type': 'text/xml',
    token : "123"
  },
});
log('Response: '+ JSON.stringify(res));
```

If you require complex authentication methods such as OAuth, OAuth2, etc., it is recommended to use packages.

## Content format

The service has some special handling for `JSON` and `XML` content types which are explained
below in detail.
For other content types, it will just try to convert the content to a plain
string and will be the responsibility of the developer to handle it.
If you are working with binary data, you probably need to download it.
Check the section [Downloading files](#downloading-files) for more information.

### JSON

If the content type is `application/json` the service will automatically convert the content
from and to JSON.

If the response is an array, it will be converted to an array.

### XML

If the content type is `application/json` the service will automatically convert the content
from and to a Javascript object using [JXON](https://developer.mozilla.org/en-US/docs/JXON): 

For example, if an external service responds with the following XML:

```
<?xml version="1.0" encoding="utf-8"?>
<request method="client.delete">
  <client_id>13</client_id>
</request>
```

It will be automatically converted to:

```json
{
  "request": {
    "@method": "client.delete",
    "client_id": 13
  }
}
```

The conversion also works the other way around. For example, for the following call:

```js
var request = {'request': {'@method':"system.current"}};
var msg = {
  url: 'https://postman-echo.com/post',
  headers: {
    'Content-Type': 'text/xml'  // needed to enforce the conversion
  },
  body: request
};
var res = svc.http.post(msg);
log('Response: '+ JSON.stringify(res));
```

The body sent to the external service will be converted to this XML:

```
<?xml version="1.0" encoding="utf-8"?>
<request method="system.current"></request>
```

# Javascript API

All methods in the Javascript API allow the following options:
- `url`: the URL you will send the request.
- `params`: an object with query parameters for the request (they go in the query string part of 
  the request).
- `headers`: an object with headers to send in the request.
  If you provide headers, these will override the ones defined in the service's configuration.
- `àuthorization`: an object with the method of authorization as explained above.
- `body`: this is the body of the request. If you are using JSON, you can directly send an object
  or array, and it will be automatically converted to a JSON string. If you are using an XML content
  type, this will be converted based on the rules defined [above](#xml).
- `settings`: an object with each option to customize the request

Check each method to see how to pass these options.

## Settings

- `connectionTimeout`: overwrites the [`Connection timeout`](#connection-timeout) configuration set on the service only
  for the request.
- `readTimeout`: overwrites the [`Read timeout`](#read-timeout) configuration set on the service only for the
  request.
- `followRedirects`: overwrites the [`Follow redirects`](#follow-redirects) configuration set on the service only for
  the request.
- `maxRedirects`: the maximum value of hosts that a request with code 3xx will pass through (default: 10)
- `fullResponse`: controls what will be set in the response. If `true` the response when calling
  an HTTP method will be an object with fields `status`, `headers` and `body`. This is important
  if you need to check status or headers in the response.
- `forceDownload`: to download files.
- `downloadSync`: to download files syncronic, blocking the execution (needed the previous flag).
- `encodeUrl`: this flag codifies the url with UTF-8 encoding. (if the url has been previously encoded should be true)
- `forceDisableCookies`: overwrites the [`Remember cookies`](#remember-cookies) configuration set on the service only for
  the request.
  (take account that this flag is the opposite than the configuration)
- `removeRefererHeaderOnRedirect`: remove the "Referer" header from the response.
- `defaultCallback`: callback function.
- `followAuthorizationHeader`: maintain the "Authorization" header when are 3xx codes.
- `followOriginalHttpMethod`: maintain the "Method" of the request when are 3xx codes.
- `useSSL`: allow configure SSL.
- `fileName`: the name of the file that would be downloaded (needs the forceDownload flag otherwise is ignored).
- `multipart`: allows sending multipart content.
- `parts`: array with the information related to the file to send

## Methods

### GET requests

You can make `GET` requests like this:

```js
var res = svc.http.get({
  url: 'https://postman-echo.com/get',
  params: {
    foo1: '1'
  },
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: "token",
    anotherHeader: "header"
  },
});
log(JSON.stringify(res)); // this will print the body of the response
```

If you need to get information of the headers, you can send the `fullResponse` flag in `true`:

```js
var res = svc.http.get({
  url: 'https://postman-echo.com/get',
  params: {
    foo1: '1'
  },
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: "token"
  },
  settings: {
    fullResponse: true
  }
});
log(JSON.stringify(res.status));
log(JSON.stringify(res.headers));
log(JSON.stringify(res.body));
```

Keep in mind that header keys will be all lower case.

If you want to overwrite some of the connection values set on the service configuration for only the request, use the 
`connectionTimeout`, `readTimeout` and `followRedirects` flags:

```js
var res = svc.http.get({
  url:'https://postman-echo.com/get',
  settings: {
    connectionTimeout: 1000,  // 1 sec
    readTimeout: 30000,       // 30 sec
    followRedirects: false    // redirects disabled
  }
});
```

#### Downloading files

Through `GET` requests it is possible to download files, and there are some specific features to
make it easier. There are three additional options that can be sent in `GET` requests:

- `forceDownload`: indicates that the resource has to be downloaded into a file instead of
  returning it in the response.
- `downloadSync`: if `true` the method won't return until the file has been downloaded, and it
  will return all the information of the file. See samples below. Default value is `false`.
- `fileName`: if provided, the file will be stored with this name.
  If empty, the file name will be calculated from the URL.
  
If you want to download a file in a synchronous way, you should do something like this:

```js
var res = svc.http.get({
  url:'https://platform-docs.slingr.io/images/vendor/logo.png',
  settings: {
      forceDownload: true,
      downloadSync: true,
      fileName: "logoSlingr.png"
  }
});

log("response: "+JSON.stringify(res));

// saves the file into a field of type File
record.field('document').val({
  id: res.fileId,
  name: res.fileName,
  contentType: res.contentType
});
sys.data.save(record);
```

If you don't want to block execution until the download is completed, you can do it asynchronously:

```js
var res = svc.http.get(
  {
    url: 'https://platform-docs.slingr.io/images/vendor/logo.png',
    settings: {
        forceDownload: true,
        fileName: "logoSlingr.png"
    }
  },
  {
    record: record  
  },
  {
    fileDownloaded: function(event, callbackData) {
      log("response: "+JSON.stringify(event.data));
    
      // saves the file into a file field
      callbackData.record.field('document').val({
        id: event.data.fileId,
        name: event.data.fileName,
        contentType: event.data.contentType
      });
      sys.data.save(callbackData.record);
    }
  }
);
```

This works like any other callback where the event is `fileDownloaded`.

### POST requests

```js
var res = svc.http.post({
  url: 'https://postman-echo.com/post',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  body: {
    name: 'test 1',
    type: 'a'
  }
});
log(JSON.stringify(res));
```

### PUT requests

You can make `PUT` requests like this:

```js
var res = svc.http.put({
  url: 'https://postman-echo.com/put',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  body: {
    type: 'b'
  }
});
log(JSON.stringify(res));
```

### PATCH requests

You can make `PATCH` requests like this:

```js
var res = svc.http.patch({
  url: 'https://postman-echo.com/patch',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  body: {
    type: 'b'
  }
});
log(JSON.stringify(res));
```

### DELETE requests

You can make `DELETE` requests like this:

```js
var res = svc.http.delete({
  url: 'https://postman-echo.com/delete',
  headers: {
    'Accept': 'application/json'
  }
});
log(JSON.stringify(res));
```

### OPTIONS requests

You can make `OPTIONS` requests like this:

```js
var res = svc.http.options({
  url: 'https://postman-echo.com/options',
  settings: {
    fullResponse: true
  }
});
log(JSON.stringify(res));
```

### HEAD requests

You can make `HEAD` requests like this:

```js
var res = svc.http.head({
  url: 'https://postman-echo.com/head',
  settings: {
    fullResponse: true
  }
  
});
log(JSON.stringify(res));
```

### Multipart requests

It is possible to send multipart request when using `POST` or `PUT`. This is specially useful when
sending files. It works like this:

```js
var request = {
    url: '.../customers/'+customerId+'/documents/'+documentId,
    settings: {
      multipart: true,
      parts: [
        {
          name: 'file',
          type: 'file',
          fileId: record.field('document').id()
        },
        {
          name: 'description',
          type: 'other',
          contentType: 'text/plain',
          content: 'this is a description of the document'
        }
      ]
    }
};
var res = svc.http.post(request);
```

As you can see, you can send one or many parts in the multipart. Each part has the following fields:

- `name`: the name of the field in the multipart.
- `type`: can be `file` if it is a file or `other` if it is any other content.
- `fileId`: this is the ID of the file in the app. Required if `type` is `file`.
- `contentType`: this is the content type of the part. Only when `type` is `other` and it is optional.
- `content`: this is the content of the type. Could be a JSON or a string. Required when `type` is `other`. 

## Events

### Webhooks

When an external service calls the webhook URL, an event will be triggered in the app that you can
catch in a listener like this one:

```js
sys.logs.info('request info: ' + JSON.stringify(event.data.requestInfo));
sys.logs.info('request headers: ' + JSON.stringify(event.data.headers));
sys.logs.info('request body: ' + JSON.stringify(event.data.body));
```

Keep in mind that header keys will be in lower case.

The field `requestInfo` contains this information:

- `method`: it is the HTTP verb, like `POST`, `GET`, etc.
- `url`: the full URL of the request, like `https://app.slingrs.io/prod/services/http`.
- `encoding`: encoding of the request, like `UTF-8`.

Keep in mind that the service will just respond with `200` status code with `ok` as the body, and 
you won't be able to provide a custom response.

### Sync Webhooks

- `method`: it is the HTTP verb, like `POST`, `GET`, etc.
- `url`: the full URL of the request, like `https://app.slingrs.io/prod/services/http/sync`.
- `encoding`: encoding of the request, like `UTF-8`.

This works exactly the same as the regular webhooks with the only difference that you can return
the response from the listener that process the webhook. For example, the listener could be like
this:

```js
var res = {
    status: app.orders.process(event.data),
    date: new Date()
};
return res;
```

Keep in mind that the response should be a valid JSON.

### Download Files from the application

- `method`: `GET`.
- `url`: the full URL of the request, like `https://app.slingrs.io/prod/services/http/download/<some-path-query>`.
- `encoding`: encoding of the request, like `UTF-8`.

This works exactly the same as the regular webhooks with the only difference that you can return
the fileId and fileName of one file to download from the browser:

```js
  sys.context.setCurrentUserRecord(sys.data.findOne("sys.users", {email: "usuario123@test.com"}))
  let user = sys.context.getCurrentUserRecord();
    // the field "file" is of type File
  return {
      fileId: user.field("file").val().id, 
      fileName: user.field("file").val().name
  }
```

Keep in mind that the response should be a valid JSON.

## About SLINGR

SLINGR is a low-code rapid application development platform that accelerates development, with robust architecture for integrations and executing custom workflows and automation.

[More info about SLINGR](https://slingr.io)

## License

This service is licensed under the Apache License 2.0. See the `LICENSE` file for more details.
