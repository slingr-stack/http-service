package io.slingr.service.http;


import io.slingr.services.HttpService;
import io.slingr.services.configurations.Configuration;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.framework.annotations.*;
import io.slingr.services.services.AppLogs;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.slingr.services.services.HttpService.defaultWebhookConverter;

@SlingrService(name = "http")
public class Http extends HttpService {

    private static final Logger logger = LoggerFactory.getLogger(Http.class);

    @ApplicationLogger
    private AppLogs appLogs;

    @ServiceProperty
    private String baseUrl;

    @ServiceConfiguration
    private Json configuration;

    @Override
    public String getApiUri() {
        return StringUtils.isNotBlank(baseUrl) ? baseUrl : "";
    }

    @Override
    public void serviceStarted() {
        logger.info("Initializing http-service");
        final String headers = configuration.string("defaultHeaders", "");
        try {
            final Json jHeaders = checkHeaders(headers);
            jHeaders.forEachMapString(httpService()::setupDefaultHeader);
        } catch (Exception ex) {
            appLogs.error(String.format("Invalid default headers defined for HTTP service. Please check them [%s]", headers));
        }
        httpService().setDefaultEmptyPath(configuration.string("emptyPath", ""));
        httpService().setRememberCookies(Configuration.parseBooleanValue(configuration.string("rememberCookies"), false));
        if (StringUtils.isBlank(baseUrl)) {
            httpService().setAllowExternalUrl(true);
        } else {
            httpService().setAllowExternalUrl(Configuration.parseBooleanValue(configuration.string("allowExternalUrl"), false));
        }
        httpService().setFollowRedirects(Configuration.parseBooleanValue(configuration.string("followRedirects"), true));
        httpService().setConnectionTimeout(configuration.integer("connectionTimeout", 5000));
        httpService().setReadTimeout(configuration.integer("readTimeout", 60000));
        logger.info(String.format("Configured HTTP service: baseUrl [%s]", baseUrl));
    }

    /**
     * Converts the string headers representation in a Json map object
     *
     * @param stringHeaders string headers list
     * @return json map object
     */
    private static Json checkHeaders(String stringHeaders) {
        final Json headers = Json.map();
        try {
            if (StringUtils.isNotBlank(stringHeaders)) {
                final String[] pairs = StringUtils.split(stringHeaders, ",");
                for (String pair : pairs) {
                    final String[] keyValue = StringUtils.split(pair, "=");

                    headers.set(keyValue[0].trim(), keyValue.length > 1 ? keyValue[1].trim() : true);
                }
            }
        } catch (Exception e) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Default headers [%s] are invalid", stringHeaders));
        }
        return headers;
    }
    @ServiceWebService(path = "/{externalService}")
    public WebServiceResponse asyncWebhook(WebServiceRequest request) {
        try {
            Json body = request.getJsonBody();
            events().send("webhook",  defaultWebhookConverter(request));
            return new WebServiceResponse("ok");
        } catch (ClassCastException cce) {
            appLogs.error("The response to the webhook from the listener is not a valid JSON");
        } catch (Exception e) {
            appLogs.error("There was an error processing webhook: " + e.getMessage(), e);
        }
        return new WebServiceResponse(Json.map(), ContentType.APPLICATION_JSON.toString());
    }
    @ServiceWebService(path = "{externalService}/sync")
    public WebServiceResponse optionsLoad(WebServiceRequest request) {
        try {
            Json options = (Json) events().sendSync("webhookSync", defaultWebhookConverter(request));
            return new WebServiceResponse(options, ContentType.APPLICATION_JSON.toString());
        } catch (ClassCastException cce) {
            appLogs.error("The response to the sync webhook from the listener is not a valid JSON");
        } catch (Exception e) {
            appLogs.error("There was an error processing sync webhook: " + e.getMessage(), e);
        }
        return new WebServiceResponse(Json.map(), ContentType.APPLICATION_JSON.toString());
    }
}
