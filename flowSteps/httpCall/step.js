/**
 * This flow step will send generic request.
 *
 * @param {text} method, This is used to config method.
 * @param {text} path, This is used to config external URL.
 * @param {string} headers, This is used to config external URL.
 * @param {string} params, This is used to config external URL.
 * @param {string} body, This is used to send body request.
 * @param {text} callbackData, This is used to send callback data.
 * @param {text} callbacks, This is used to send callbacks.
 */
step.httpCall = function (method, path, headers, params, body, callbackData, callbacks) {

	sys.logs.debug('[http-service] path from: ' + path);

	body = body ? JSON.parse(body) : null
	headers = headers ? JSON.parse(headers) : null
	params = params ? JSON.parse(params) : null

	var options = {
		path: path,
		params:params,
		headers:headers,
		body: body
	}

    switch (method) {
    	case 'get':
    		return endpoint._get(options, callbackData, callbacks);
    	case 'post':
    		return endpoint._post(options, callbackData, callbacks);
    	case 'delete':
    		return endpoint._delete(options, callbackData, callbacks);
    	case 'put':
    		return endpoint._put(options, callbackData, callbacks);
    	case 'connect':
    		return endpoint._connect(options, callbackData, callbacks);
    	case 'head':
    		return endpoint._head(options, callbackData, callbacks);
    	case 'options':
    		return endpoint._options(options, callbackData, callbacks);
    	case 'patch':
    		return endpoint._patch(options, callbackData, callbacks);
    	case 'trace':
    		return endpoint._trace(options, callbackData, callbacks);
    	default:
            return null;
    }

};