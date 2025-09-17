package io.slingr.service.http;

import io.slingr.services.HttpService;
import io.slingr.services.configurations.Configuration;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.framework.annotations.*;
import io.slingr.services.services.AppLogs;
import io.slingr.services.services.rest.DownloadedFile;
import io.slingr.services.services.rest.RestMethod;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static io.slingr.services.services.HttpService.defaultWebhookConverter;

@SlingrService(name = "http")
public class Http extends HttpService {
    private static final Logger logger = LoggerFactory.getLogger(Http.class);

    private static final String SERVICE_NAME = "http";

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
        logger.info("Initializing service [{}]", SERVICE_NAME);
        appLogs.info(String.format("Initializing service [%s]", SERVICE_NAME));
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
        logger.info("Configured service [{}]: baseUrl [{}]", SERVICE_NAME, baseUrl);
    }

    @Override
    public void serviceStopped(String cause) {
        logger.error("Stopping service [{}] with cause [{}]", SERVICE_NAME, cause);
        appLogs.info(String.format("Stopping service [%s]", SERVICE_NAME));
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

    @ServiceWebService(path = "/sync")
    public WebServiceResponse syncWebhook(WebServiceRequest request) {
      return syncWebhookCustom(request);
    }

    @ServiceWebService(path = "/sync/{externalService}")
    public WebServiceResponse syncWebhookCustom(WebServiceRequest request) {
        logger.info("Webhook sync received for service [{}]", SERVICE_NAME);
        try {
            Json webhookConverted = defaultWebhookConverter(request);
            webhookConverted.set("path", request.getPath().replace("/sync", ""));
            Json options = (Json) events().sendSync("webhookSync", webhookConverted);
            return new WebServiceResponse(options, ContentType.APPLICATION_JSON.toString());
        } catch (ClassCastException cce) {
            appLogs.error("The response to the sync webhook from the listener is not a valid JSON");
        } catch (Exception e) {
            appLogs.error("There was an error processing sync webhook: " + e.getMessage(), e);
        }
        return new WebServiceResponse(500, Json.map(), ContentType.APPLICATION_JSON.toString());
    }

    @ServiceWebService(path = "/{externalService}")
    public WebServiceResponse asyncWebhook(WebServiceRequest request) {
        logger.info("Webhook received for service [{}]", SERVICE_NAME);
        try {
            events().send("webhook",  defaultWebhookConverter(request));
            return new WebServiceResponse("Ok");
        } catch (ClassCastException cce) {
            appLogs.error("The response to the webhook from the listener is not a valid JSON");
        } catch (Exception e) {
            appLogs.error("There was an error processing webhook: " + e.getMessage(), e);
        }
        return new WebServiceResponse(Json.map(), ContentType.APPLICATION_JSON.toString());
    }

    @ServiceWebService(path = "/download/{fileId}", methods = {RestMethod.GET})
    public WebServiceResponse downloadFile(WebServiceRequest request) {
        logger.info("Webhook sync for download file received, for service [{}]", SERVICE_NAME);
        try {
            Json webhookConverted = defaultWebhookConverter(request);
            webhookConverted.set("path", request.getPath().replace("/download", ""));
            Json options = (Json) events().sendSync("downloadFile", webhookConverted);
            if (options != null) {
                if (options.contains("fileId") && options.string("fileId") != null) {
                    Json fileMetadata = files().metadata(options.string("fileId"));
                    DownloadedFile downloadedFile = files().download(options.string("fileId"));
                    final byte[] fileBytes = Http.getFileBytes(downloadedFile);
                    Json headers = downloadedFile.headers();
                    headers.set("Content-Type", fileMetadata.contains("contentType") ? fileMetadata.string("contentType") : "application/octet-stream");
                    headers.set("Content-Length", fileMetadata.contains("length") ? fileMetadata.string("length") : null);
                    String fileName = options.contains("fileName") ?
                            options.string("fileName").split(" ")[0] :
                            (fileMetadata.contains("fileName") ?
                                    fileMetadata.string("fileName") :
                                    "file");
                    headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    return new WebServiceResponse(200, fileBytes, headers);
                }
            }
            return new WebServiceResponse(404, Json.map(), ContentType.APPLICATION_JSON.toString());
        } catch (ClassCastException cce) {
            appLogs.error("The response to the sync webhook from the listener is not a valid JSON");
        } catch (Exception e) {
            appLogs.error("There was an error processing sync webhook: " + e.getMessage(), e);
        }
        return new WebServiceResponse(500, Json.map(), ContentType.APPLICATION_JSON.toString());
    }

    private static byte[] getFileBytes(DownloadedFile downloadedFile) throws IOException {
        ByteArrayOutputStream buffer;
        try (InputStream file = downloadedFile.file()) {
            buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = file.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}