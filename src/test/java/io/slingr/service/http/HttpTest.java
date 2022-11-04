package io.slingr.service.http;


import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.tests.SvcTests;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * <p>Test over the Http class and RequestBin
 *
 * <p>Created by lefunes on 03/28/16.
 */
@Ignore("For dev proposes")
public class HttpTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpTest.class);

    private static SvcTests test;

    @BeforeClass
    public static void init() throws Exception {
        test = SvcTests.start(new io.slingr.service.http.Runner(), "test.properties");
    }

    @Test
    public void testPostRequest() throws Exception {
        // build request
        final Json req = Json.map();
        req.set("body", Json.map().set("field1", "A").set("field2", "B"));
        req.set("path", "");
        req.set("headers", Json.map().set("token", "abc"));

        // test request
        Json res = test.executeFunction("_post", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertEquals("ok\n", res.string("body"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        // test request - full response
        req.set("fullResponse", "true");
        res = test.executeFunction("_post", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertEquals("ok\n", res.string("body"));
        // it has headers
        assertNotNull(res.json("headers"));
        assertFalse(res.json("headers").isEmpty());
        // it has a 200 response code
        assertEquals(200, (int) res.integer("status"));

        // test request
        req.set("body", Json.map().set("END", "ok"));
        req.set("path", "");
        req.set("headers", Json.map());
        res = test.executeFunction("_post", req);
        assertNotNull(res);
        assertFalse(res.is(Parameter.EXCEPTION_FLAG));
        // body is 'ok'
        assertEquals("ok\n", res.string("body"));
        // it is not a full response
        assertFalse(res.is("fullResponse"));

        logger.info("-- END");
    }
}