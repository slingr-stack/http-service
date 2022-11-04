---
title: HTTP endpoint
keywords: 
last_updated: April 20, 2017
tags: []
summary: "Detailed description of the API of the HTTP endpoint."
---

## Overview

The HTTP endpoint allows to make HTTP requests as well as receive HTTP request from
other servers. This is the list of features:

- Send HTTP requests (`GET`, `PUT`, `POST`, etc.) to other servers
- Receive external HTTP requests (`GET`, `PUT`, `POST`, etc.)
- Download files
- Support for basic and digest authentication
- Support for cookies

In many cases you can use the HTTP endpoint to call external services where an official
endpoint does not exist. This will work as long as they provide a REST HTTP API.

## Configuration

### Base URL

If all the request you will be doing through this endpoint have a common root URL, you can
put it here to avoid having to pass it on every request.

You can also leave this empty and provide the full URL on each request.

### Default headers

Allows to define headers that will be added to all requests done through this endpoint. The
format is `key=value` and you can specify several headers separated by commas:

```
Content-Type=application/json,Accept=application/json
```

### Empty path

If the path is empty when a request is done through the endpoint, this is the default path
that will be used. You can leave this empty if you don't want any default path.

### Authorization

This is the authorization used to make requests. Options are:

- `No authorization`: no authorization will be done. This is the case for public services or when
  you have a custom authorization method (for example a token sent in headers).
- `Basic authorization`: basic authorization will be used. You will need to provide username 
  and password. 
- `Digest authorization`: digest authorization will be used. You will need to provide username
  and password.
  
### Username

Username to access the external service. Needed when using `Basic authorization` or 
`Digest authorization`.

### Password

Password to access the external service. Needed when using `Basic authorization` or 
`Digest authorization`.

### Remember cookies

Enable this flag if you want to use a basic system to exchange cookies with the external 
service, where the endpoint will send the last received cookies in subsequent requests.

### Connection timeout

This is the maximum time the endpoint waits to perform the connection to an external 
service. If it times out, an exception is thrown and the request cancelled. 
Default value: 5000 ms (5 sec). Set to zero for to wait indefinitely. 

### Read timeout

This is the maximum time the endpoint waits to receive the response to a request to 
an external service. If it times out, an exception is thrown and the request 
cancelled. Default value: 60000 ms (60 sec). Set to zero to wait indefinitely.

### Follow redirects

If it is enabled, the endpoint will automatically redirect when it receives a 
`3xx` HTTP status code as response to a request.

### Webhook URL

This is the URL the endpoint will be listening for requests, which will be sent as events
to the app.

### Sync Webhook URL

This is the URL the endpoint will be listening for requests, which will be sent as events
to the app.

The difference with the webhooks above is that in this case the listener should return a
JSON object that will be return to the caller.

## Quick start

You can make a simple `GET` request like this:

```js
var res = app.endpoints.http.get({
  path: '/orders',
  params: {
    type: 'a'
  },
  headers: {
    token: token
  }
});
res.items.forEach(function(item) {
  log('item: '+item.name);
});
```

Also a `POST` request can send information like this:

```js
var res = app.endpoints.http.post({
  path: '/companies',
  headers: {
    token: token
  },
  body: {
    name: 'test1'
  }
});
log('response from server: '+JSON.stringify(res));
```

If the response code is not `2XX` you can catch the exception:

```js
try {
  app.endpoints.http.post(msg);
} catch (e) {
  log('status code: '+e.additionalInfo.status);
  log('headers: '+JSON.stringify(e.additionalInfo.headers));
  log('body: '+JSON.stringify(e.additionalInfo.body));
}
```

In webhooks you will have all the information of the request in the event:

```js
sys.logs.info('request info: ' + JSON.stringify(event.data.requestInfo));
sys.logs.info('request headers: ' + JSON.stringify(event.data.headers));
sys.logs.info('request body: ' + JSON.stringify(event.data.body));
```

## Content format

The endpoint has some special handling for `JSON` and `XML` content types which are explained
below in detail. For other content types it will just try to convert the content to a plain
string and will be responsibility of the developer to handle it.

If you are working with binary data you probably need to download it. Check the section 
[Downloading files](#donwloading-files) for more information.

### JSON

If the content type is `application/json` the endpoint will automatically convert the content
from and to JSON. For example when you make a `GET` request like this:

```js
var res = app.endpoints.get('/companies/'+companyId);
log('company name: '+res.name);
```

If the response is an array, it will be converted to an array:

```js
var res = app.endpoints.get('/repositories');
res.forEach(function(repo) {
  log('repo: '+repo.name);
});
```

Same when you send data through `POST` or `PUT` requests:

```js
var res = app.endpoints.http.post({
  path: '/companies',
  headers: {
    token: token
  },
  body: {
    name: 'test1'
  }
});
```

In this case the object is automatically converted to a JSON string.

For webhook events the same conversion will be done:

```js
log('name: '+event.data.body.name);
```

### XML

If the content type is `application/json` the endpoint will automatically convert the content
from and to a Javascript object using [JXON](https://developer.mozilla.org/en-US/docs/JXON): 

For example if an external service responds with the following XML:

```
<?xml version="1.0" encoding="utf-8"?>
<request method="client.delete">
  <client_id>13</client_id>
</request>
```

It will be automatically converted to:

```js
{
  "request": {
    "@method": "client.delete",
    "client_id": 13
  }
}
```

The conversion also works the other way around. For example for the following call:

```js
var request = {'request': {'@method':"system.current"}};
var msg = {
  headers: {
    'Content-Type': 'text/xml'  // needed to enforce the conversion
  },
  body: request
};
var response = app.endpoints.http.post(msg);
```

The body sent to the external service will be converted to this XML:

```
<?xml version="1.0" encoding="utf-8"?>
<request method="system.current"></request>
```

Here are some considerations when converting from/to XML:

{% include custom/http_endpoint_xml_conversion.html %}

## Javascript API

All methods in the Javascript API allow the following options:

- `path`: the URL you will send the request. Keep in mind that if you configured a `Base URL` in
  the endpoint this path will be appended to the URL.
- `params`: an object with query parameters for the request (they go in the query string part of 
  the request).
- `headers`: an object with headers to send in the request. If you provide headers these will 
  override the ones defined in the endpoint's configuration.
- `body`: this is the body of the request. If you are using JSON, you can directly send an object
  or array and it will be automatically converted to a JSON string. If you are using XML content
  type, this will be converted based on the rules defined [above](#xml).
- `fullResponse`: controls what will be set in the response. If `true` the response when calling
  an HTTP method will be an object with fields `status`, `headers` and `body`. This is important 
  if you need to check status or headers in the response.
- `connectionTimeout`: overwrites the [`Connection timeout`](#connection-timeout) configuration set on the endpoint only 
for the request. 
- `readTimeout`: overwrites the [`Read timeout`](#read-timeout) configuration set on the endpoint only for the 
request.
- `followRedirects`: overwrites the [`Follow redirects`](#follow-redirects) configuration set on the endpoint only for 
the request. 

Check each method to see how to pass these options.

### GET requests

You can make `GET` requests like this:

```js
var res = app.endpoints.http.get({
  path: '/data/companies',
  params: {
    type: 'a'
  },
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: token
  }
});
log(JSON.stringify(res)); // this will print the body of the response
```

If you need to get information of the headers you can send the `fullResponse` flag in `true`:

```js
var res = app.endpoints.http.get({
  path: '/data/companies',
  params: {
    type: 'a'
  },
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: token
  },
  fullResponse: true
});
log(JSON.stringify(res.status));
log(JSON.stringify(res.headers));
log(JSON.stringify(res.body));
```

Keep in mind that headers keys will be all lower case.

You can also use a shortcut:

```js
var res = app.endpoints.http.get('/data/companies');
```

If you want to overwrite some of the connection values set on the endpoint configuration for only the request, use the 
`connectionTimeout`, `readTimeout` and `followRedirects` flags:

```js
var res = app.endpoints.http.get({
  path: '/data/companies',
  connectionTimeout: 1000,  // 1 sec
  readTimeout: 30000,       // 30 sec
  followRedirects: false    // redirects disabled
});
```

#### Downloading files

Through `GET` requests it is possible to download files and there are some specific features to
make it easier. There are three additional options that can be sent in `GET` requests:

- `forceDownload`: indicates that the resource has to be downloaded into a file instead of
  returning it in the response.
- `downloadSync`: if `true` the method won't return until the file has been downloaded and it
  will return all the information of the file. See samples below. Default value is `false`.
- `fileName`: if provided, the file will be stored with this name. If empty the file name will be
  calculated from the URL.
  
If you want to download a file in a synchronous way you should do something like this:

```js
var res = app.endpoints.http.get({
  path: '/images/client_400x400.png',
  forceDownload: true,
  downloadSync: true
});

log("response: "+JSON.stringify(res));

// saves the file into a file field
record.field('document').val({
  id: res.fileId,
  name: res.fileName,
  contentType: res.contentType
});
sys.data.save(record);
```

If you don't want to block execution until the download is completed, you can do it asynchronously:

```js
var res = app.endpoints.http.get(
  {
    path: '/images/client_400x400.png',
    forceDownload: true
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

This works like any other [endpoint callback]({{site.baseurl}}/app_development_model_endpoints.html#callbacks)
where the event is 'fileDownloaded'.

### POST requests

You can make `POST` requests like this:

```js
var res = app.endpoints.http.post({
  path: '/data/companies',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: token
  },
  body: {
    name: 'test 1',
    type: 'a'
  }
});
log(JSON.stringify(res));
```

You can also use a shortcut:

```js
var body = {
  name: 'test 1',
  type: 'a'
};
var res = app.endpoints.http.post('/data/companies', body);
```

### PUT requests

You can make `PUT` requests like this:

```js
var res = app.endpoints.http.put({
  path: '/data/companies/5506fc3dc2eee3b1a70263b3',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: token
  },
  body: {
    type: 'b'
  }
});
log(JSON.stringify(res));
```

You can also use a shortcut:

```js
var body = {
  type: 'b'
};
var res = app.endpoints.http.put('/data/companies/5506fc3dc2eee3b1a70263b3', body);
```

### PATCH requests

You can make `PATCH` requests like this:

```js
var res = app.endpoints.http.patch({
  path: '/data/companies/5506fc3dc2eee3b1a70263b3',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    token: token
  },
  body: {
    type: 'b'
  }
});
log(JSON.stringify(res));
```

You can also use a shortcut:

```js
var body = {
  type: 'b'
};
var res = app.endpoints.http.patch('/data/companies/5506fc3dc2eee3b1a70263b3', body);
```

### DELETE requests

You can make `DELETE` requests like this:

```js
var res = app.endpoints.http.delete({
  path: '/data/companies/5506fc3dc2eee3b1a70263b3',
  headers: {
    'Accept': 'application/json',
    token: token
  }
});
log(JSON.stringify(res));
```

You can also use a shortcut:

```js
var res = app.endpoints.http.delete('/data/companies/5506fc3dc2eee3b1a70263b3');
```

### OPTIONS requests

You can make `OPTIONS` requests like this:

```js
var res = app.endpoints.http.options({
  path: '/data/companies/5506fc3dc2eee3b1a70263b3',
  headers: {
    token: token
  },
  fullResponse: true
});
log(JSON.stringify(res));
```

You can also use a shortcut:

```js
var res = app.endpoints.http.options('/data/companies/5506fc3dc2eee3b1a70263b3');
```

### HEAD requests

You can make `HEAD` requests like this:

```js
var res = app.endpoints.http.head({
  path: '/data/companies/5506fc3dc2eee3b1a70263b3',
  headers: {
    token: token
  },
  fullResponse: true
});
log(JSON.stringify(res));
```

You can also use a shortcut:

```js
var res = app.endpoints.http.head('/data/companies/5506fc3dc2eee3b1a70263b3');
```

### Multipart requests

It is possible to send multipart request when using `POST` or `PUT`. This is specially useful when
sending files. It works like this:

```js
var request = {
    path: '/customers/'+customerId+'/documents/'+documentId,
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
};
var res = app.endpoints.http.post(request);
```

As you can see you can send one or many parts in the multipart. Each part has the following fields:

- `name`: the name of the field in the multipart.
- `type`: can be `file` if it is a file or `other` if it is any other content.
- `fileId`: this is the ID of the file in the app. Required if `type` is `file`.
- `contentType`: this is the content type of the part. Only when `type` is `other` and it is optional.
- `content`: this is the content of the type. Could be a JSON or a string. Required when `type` is `other`. 

## Events

### Webhooks

When an external service calls the webhook URL, an event will triggered in the app that you can
catch in an [endpoint listener]({{site.baseurl}}/app_development_model_listeners.html#endpoint-listeners)
like this one:

```js
sys.logs.info('request info: ' + JSON.stringify(event.data.requestInfo));
sys.logs.info('request headers: ' + JSON.stringify(event.data.headers));
sys.logs.info('request body: ' + JSON.stringify(event.data.body));
```

Keep in mind that headers keys will be in lower case.

The field `requestInfo` contains this information:

- `method`: it is the HTTP verb, like `POST`, `GET`, etc.
- `url`: the full URL of the request, like `https://app.slingrs.io/prod/endpoints/http`.
- `encoding`: encoding of the request, like `UTF-8`.

Keep in mind that the endpoint will just respond with `200` status code with `ok` as the body and 
you won't be able to provide a custom response.

### Sync Webhooks

This work exactly the same as the regular webhooks with the only difference that you can return
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

## About SLINGR

SLINGR is a low-code rapid application development platform that accelerates development, with robust architecture for integrations and executing custom workflows and automation.

[More info about SLINGR](https://slingr.io)

## License

This endpoint is licensed under the Apache License 2.0. See the `LICENSE` file for more details.
