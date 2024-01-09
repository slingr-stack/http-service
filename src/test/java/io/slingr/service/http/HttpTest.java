package io.slingr.service.http;

import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.tests.ServiceTests;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

@SuppressWarnings("SpellCheckingInspection")
public class HttpTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpTest.class);

    private static ServiceTests test;

    @BeforeClass
    public static void init() throws Exception {
        test = ServiceTests.start(new io.slingr.service.http.Runner(), "test.properties");
    }

    @Test
    public void testGetRequest() {
        logger.info("-- INIT --");
        // build request
        final Json req = Json.map();
        req.set("body", Json.map().set("field1", "A").set("field2", "B"));
        req.set("url", "https://postman-echo.com/get");
        req.set("headers", Json.map().set("token", "abc"));

        // test request
        Json res = test.executeFunction("get", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertNull(res.string("body"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        // test request - full response
        req.set("settings", Json.map().set("fullResponse", "true"));
        res = test.executeFunction("get", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertNull(Json.parse(res.string("body")).string("data"));
        // it has headers
        assertNotNull(res.json("headers"));
        assertFalse(res.json("headers").isEmpty());
        // it has a 200 response code
        assertEquals(200, (int) res.integer("status"));

        // test request
        req.set("body", Json.map().set("END", "ok"));
        req.set("path", "");
        req.set("headers", Json.map());
        res = test.executeFunction("get", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertNull(Json.parse(res.string("body")).string("data"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        logger.info("-- END --");
    }

    @Test
    public void testPostRequest() {
        // build request
        final Json req = Json.map();
        req.set("body", Json.map().set("field1", "A").set("field2", "B"));
        req.set("url", "https://postman-echo.com/post");
        req.set("headers", Json.map().set("token", "abc"));

        // test request
        Json res = test.executeFunction("post", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertNull(res.string("body"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        // test request - full response
        req.set("settings", Json.map().set("fullResponse", "true"));
        res = test.executeFunction("post", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        Json bodyExpected = Json.parse("{\"args\":{},\"data\":{\"field1\":\"A\",\"field2\":\"B\"},\"files\":{},\"form\":{},\"headers\":{\"x-forwarded-proto\":\"https\",\"x-forwarded-port\":\"443\",\"host\":\"postman-echo.com\",\"x-amzn-trace-id\":\"Root=1-64230965-44aa990f6ec58b280e7538c3\",\"content-length\":\"27\",\"accept\":\"application/json,application/x-www-form-urlencoded,text/xml,application/xml,application/atom+xml,application/svg+xml,application/xhtml+xml,text/html,text/plain,multipart/form-data\",\"user-agent\":\"Jersey/2.39 (Apache HttpClient 4.5.13)\",\"content-type\":\"application/json\",\"token\":\"abc\",\"accept-encoding\":\"gzip,deflate\"},\"json\":{\"field1\":\"A\",\"field2\":\"B\"},\"url\":\"https://postman-echo.com/post\"}");
        assertEquals(bodyExpected.string("data"), Json.parse(res.string("body")).string("data"));
        // it has headers
        assertNotNull(res.json("headers"));
        assertFalse(res.json("headers").isEmpty());
        // it has a 200 response code
        assertEquals(200, (int) res.integer("status"));

        // test request
        req.set("body", Json.map().set("END", "ok"));
        req.set("path", "");
        req.set("headers", Json.map());
        res = test.executeFunction("post", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        Json bodyExpected2 = Json.parse("{\"END\":\"ok\"}");
        assertEquals(bodyExpected2.toString(), Json.parse(res.string("body")).string("data"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        logger.info("-- END");
    }

    @Test
    public void testPutRequest() {
        logger.info("-- INIT --");
        // build request
        final Json req = Json.map();
        req.set("body", Json.map().set("field1", "A").set("field2", "B"));
        req.set("url", "https://postman-echo.com/put");
        req.set("headers", Json.map().set("token", "abc"));

        // test request
        Json res = test.executeFunction("put", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertNull(res.string("body"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        // test request - full response
        req.set("settings", Json.map().set("fullResponse", "true"));
        res = test.executeFunction("put", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        Json bodyExpected = Json.parse("{\"args\":{},\"data\":{\"field1\":\"A\",\"field2\":\"B\"},\"files\":{},\"form\":{},\"headers\":{\"x-forwarded-proto\":\"https\",\"x-forwarded-port\":\"443\",\"host\":\"postman-echo.com\",\"x-amzn-trace-id\":\"Root=1-64230965-44aa990f6ec58b280e7538c3\",\"content-length\":\"27\",\"accept\":\"application/json,application/x-www-form-urlencoded,text/xml,application/xml,application/atom+xml,application/svg+xml,application/xhtml+xml,text/html,text/plain,multipart/form-data\",\"user-agent\":\"Jersey/2.39 (Apache HttpClient 4.5.13)\",\"content-type\":\"application/json\",\"token\":\"abc\",\"accept-encoding\":\"gzip,deflate\"},\"json\":{\"field1\":\"A\",\"field2\":\"B\"},\"url\":\"https://postman-echo.com/put\"}");
        assertEquals(bodyExpected.string("data"), Json.parse(res.string("body")).string("data"));
        // it has headers
        assertNotNull(res.json("headers"));
        assertFalse(res.json("headers").isEmpty());
        // it has a 200 response code
        assertEquals(200, (int) res.integer("status"));

        // test request
        req.set("body", Json.map().set("END", "ok"));
        req.set("path", "");
        req.set("headers", Json.map());
        res = test.executeFunction("put", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        Json bodyExpected2 = Json.parse("{\"END\":\"ok\"}");
        assertEquals(bodyExpected2.toString(), Json.parse(res.string("body")).string("data"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        logger.info("-- END --");
    }

    @Test
    public void testDeleteRequest() {
        logger.info("-- INIT --");
        // build request
        final Json req = Json.map();
        req.set("body", Json.map().set("field1", "A").set("field2", "B"));
        req.set("url", "https://postman-echo.com/delete");
        req.set("headers", Json.map().set("token", "abc"));

        // test request
        Json res = test.executeFunction("delete", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertNull(res.string("body"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        // test request - full response
        req.set("settings", Json.map().set("fullResponse", "true"));
        res = test.executeFunction("delete", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        Json bodyExpected = Json.map();
        assertEquals(bodyExpected.toString(), Json.parse(res.string("body")).string("data"));
        // it has headers
        assertNotNull(res.json("headers"));
        assertFalse(res.json("headers").isEmpty());
        // it has a 200 response code
        assertEquals(200, (int) res.integer("status"));

        // test request
        req.set("body", Json.map().set("END", "ok"));
        req.set("path", "");
        req.set("headers", Json.map());
        res = test.executeFunction("delete", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertEquals(bodyExpected.toString(), Json.parse(res.string("body")).string("data"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        logger.info("-- END --");
    }
}