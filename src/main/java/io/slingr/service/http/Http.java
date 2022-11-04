package io.slingr.service.http;


import io.slingr.svcs.HttpSvc;
import io.slingr.svcs.configurations.Configuration;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.framework.annotations.*;
import io.slingr.svcs.services.AppLogs;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.Strings;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>HTTP serice
 *
 * <p>Created by dgaviola on 08/22/15.
 */
@SlingrService(name = "http", functionPrefix = "_")
public class Http extends HttpSvc {
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
    public void svcStarted() {
        final String headers = configuration.string("defaultHeaders", "");
        try {
            final Json jHeaders = checkHeaders(headers);
            jHeaders.forEachMapString(httpService()::setupDefaultHeader);
        } catch (Exception ex){
            appLogs.error(String.format("Invalid default headers defined for HTTP service. Please check them [%s]", headers));
        }

        httpService().setDefaultEmptyPath(configuration.string("emptyPath", ""));

        httpService().setRememberCookies(Configuration.parseBooleanValue(configuration.string("rememberCookies"), false));

        if(StringUtils.isBlank(baseUrl)){
            httpService().setAllowExternalUrl(true);
        } else {
            httpService().setAllowExternalUrl(Configuration.parseBooleanValue(configuration.string("allowExternalUrl"), false));
        }

        httpService().setFollowRedirects(Configuration.parseBooleanValue(configuration.string("followRedirects"), true));
        httpService().setConnectionTimeout(configuration.integer("connectionTimeout", 5000));
        httpService().setReadTimeout(configuration.integer("readTimeout", 60000));

        final String authType = configuration.string("authType", "");

        if(StringUtils.isNotBlank(authType)) {
            final String username = configuration.string("username", "");
            final String password = configuration.string("password", "");

            if ("basic".equalsIgnoreCase(authType)) {
                httpService().setupBasicAuthentication(username, password);

                logger.info(String.format("Configured HTTP Basic authentication: username [%s] - password [%s]", username, Strings.maskToken(password)));
            } else if ("digest".equalsIgnoreCase(authType)) {
                httpService().setupDigestAuthentication(username, password);

                logger.info(String.format("Configured HTTP Digest authentication: username [%s] - password [%s]", username, Strings.maskToken(password)));
            } else {
                logger.info("Configured without HTTP authentication");
            }
        }

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
            if (StringUtils.isNotBlank(stringHeaders)){
                final String[] pairs = StringUtils.split(stringHeaders, ",");
                for (String pair : pairs) {
                    final String[] keyValue = StringUtils.split(pair, "=");

                    headers.set(keyValue[0].trim(), keyValue.length > 1 ? keyValue[1].trim() : true);
                }
            }
        } catch (Exception e) {
            throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Default headers [%s] are invalid", stringHeaders));
        }
        return headers;
    }

    @ServiceWebService(path = "/sync")
    public WebServiceResponse optionsLoad(WebServiceRequest request) {
        try {
            Json body = request.getJsonBody();
            Json options = (Json) events().sendSync("webhookSync", body);
            return new WebServiceResponse(options, ContentType.APPLICATION_JSON.toString());
        } catch (ClassCastException cce) {
            appLogs.error("The response to the sync webhook from the listener is not a valid JSON");
        } catch (Exception e) {
            appLogs.error("There was an error processing sync webhook: " + e.getMessage(), e);
        }
        return new WebServiceResponse(Json.map(), ContentType.APPLICATION_JSON.toString());
    }
}
