/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EvaluatePreconditionsTest {

    private static final Date DATE_OLD = new Date();
    private static final Date DATE_NEW = new Date(DATE_OLD.getTime() + 60 * 60 * 1000L);
    private static final EntityTag ETAG_OLD = new EntityTag("helloworld", true);
    private static final EntityTag ETAG_NEW = new EntityTag("xyz", true);

    private SimpleDateFormat dateFormat = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    private SimpleService service;

    @Before
    public void setUp() {
        service = new SimpleService();
        service.setEntityTag(ETAG_OLD);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        service.setLastModified(DATE_OLD);
    }

    @Test
    public void testUnconditional200() {
        final Request request = getRequest();
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testIfModified200() {
        service.setLastModified(DATE_NEW);
        final Request request = getRequest(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(DATE_OLD));
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testIfNoneMatch304() {
        final Request request = getRequest(HttpHeaders.IF_NONE_MATCH, ETAG_OLD.toString());
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    }

    @Test
    public void testIfNoneMatch200() {
        final Request request = getRequest(HttpHeaders.IF_NONE_MATCH, ETAG_NEW.toString());
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testIfModified304() {
        final Request request = getRequest(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(DATE_NEW));
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    }

    @Test
    public void testIfNoneMatchIfModified304() {
        final Request request = getRequest(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(DATE_OLD),
                                           HttpHeaders.IF_NONE_MATCH, ETAG_OLD.toString());
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    }

    @Test
    public void testIfNoneMatchIfModified200() {
        // RFC 2616 / section 14.26
        // "If none of the entity tags match, then the server MAY perform the requested method as
        // if the If-None-Match header field did not exist, but MUST also ignore any If-Modified-Since
        // header field(s) in the request. That is, if no entity tags match, then the server MUST NOT
        // return a 304 (Not Modified) response."
        final Request request = getRequest(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(DATE_OLD),
                                           HttpHeaders.IF_NONE_MATCH, ETAG_NEW.toString()); // ETags don't
                                                                                            // match,
                                                                                            // If-Modified-Since
                                                                                            // must be ignored
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testIfNoneMatchIfModified200Two() {
        // RFC 2616 / section 14.26
        // "If any of the entity tags match, the entity tag of the entity that would have been returned
        // in the response to a similar GET request (without the If-None-Match header) on that resource,
        // or if "*" is given and any current entity exists for that resource, then the server MUST NOT
        // perform the requested method, unless required to do so because the resource's modification date
        // fails to match that supplied in an If-Modified-Since header field in the request"
        service.setLastModified(DATE_NEW);
        final Request request = getRequest(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(DATE_OLD),
                                           HttpHeaders.IF_NONE_MATCH, ETAG_NEW.toString()); // ETags match,
                                                                                            // but resource
                                                                                            // has new date
        final Response response = service.perform(request);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    protected Request getRequest(final String... headers) {
        final MessageImpl message = new MessageImpl();
        final Map<String, List<String>> map = new HashMap<>();
        message.put(Message.PROTOCOL_HEADERS, map);
        for (int i = 0; i < headers.length; i += 2) {
            final List<String> l = new ArrayList<>(1);
            l.add(headers[i + 1]);
            map.put(headers[i], l);
        }
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        return new RequestImpl(message);
    }
}
