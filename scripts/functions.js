
///////////////////////////////////
// Public API - Generic Functions
//////////////////////////////////

service.get = function(url, callbackData, callbacks) {
    var options = checkHttpOptions(url, {});
    return service._get(options, callbackData, callbacks);
};

service.post = function(url, options) {
    options = checkHttpOptions(url, options);
    return service._post(options);
};

service.put = function(url, options) {
    options = checkHttpOptions(url, options);
    return service._put(options);
};

service.patch = function(url, options) {
    options = checkHttpOptions(url, options);
    return service._patch(options);
};

service.delete = function(url) {
    var options = checkHttpOptions(url, {});
    return service._delete(options);
};

service.head = function(url) {
    var options = checkHttpOptions(url, {});
    return service._head(options);
};

service.options = function(url) {
    var options = checkHttpOptions(url, {});
    return service._options(options);
};

/////////////////////////////////////
//  Private helpers
////////////////////////////////////

var checkHttpOptions = function (url, options) {
    options = options || {};
    if (!!url) {
        if (isObject(url)) {
            // take the 'url' parameter as the options
            options = url || {};
        } else {
            if (!!options.path || !!options.params || !!options.body) {
                // options contains the http package format
                options.path = url;
            } else {
                // create html package
                options = {
                    path: url,
                    body: options
                }
            }
        }
    }
    return options;
};

var isObject = function (obj) {
    return !!obj && stringType(obj) === '[object Object]'
};

var stringType = Function.prototype.call.bind(Object.prototype.toString);