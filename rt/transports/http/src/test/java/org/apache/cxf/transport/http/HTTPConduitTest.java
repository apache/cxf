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

package org.apache.cxf.transport.http;


import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class HTTPConduitTest extends Assert {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() {
    }

    /**
     * Generates a new message.
     */
    private Message getNewMessage() {
        Message message = new MessageImpl();
        Map<String, List<String>> headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        List<String> contentTypes = new ArrayList<String>();
        contentTypes.add("text/xml");
        contentTypes.add("charset=utf8");
        headers.put("content-type", contentTypes);
        message.put(Message.PROTOCOL_HEADERS, headers);
        return message;
    }

    /**
     * This test class is a Basic Auth Supplier with a
     * preemptive UserPass.
     */
    class BasicAuthSupplier extends HttpBasicAuthSupplier {
        public UserPass getPreemptiveUserPass(
                String conduitName, URL url, Message m) {
            return createUserPass("Gandalf", "staff");
        }
        public UserPass getUserPassForRealm(
                String conduitName, URL url, Message m, String r) {
            return null;
        }

    }

    /**
     * This test verfies that the "getTarget() call returns the correct
     * EndpointReferenceType for the given endpoint address.
     */
    @Test
    public void testGetTarget() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        EndpointReferenceType target =
            EndpointReferenceUtils.getEndpointReference(
                    "http://nowhere.com/bar/foo");

        // Test call
        EndpointReferenceType ref = conduit.getTarget();

        assertNotNull("unexpected null target", ref);
        assertEquals("unexpected target",
                     EndpointReferenceUtils.getAddress(ref),
                     EndpointReferenceUtils.getAddress(target));

        assertEquals("unexpected address",
                     conduit.getAddress(),
                     "http://nowhere.com/bar/foo");
        assertNull("unexpected upfront URL",
                    conduit.getURL(false));
        assertEquals("unexpected on-demand URL",
                     conduit.getURL().getPath(),
                     "/bar/foo");
    }
    
    @Test
    public void testCXF2370() throws Exception {
        String origNonce = "MTI0ODg3OTc5NzE2OTplZGUyYTg0Yzk2NTFkY2YyNjc1Y2JjZjU2MTUzZmQyYw==";
        String fullHeader = "Digest realm=\"MyCompany realm.\", qop=\"auth\","
            + "nonce=\"" + origNonce + "\"";
        Map<String, String> map = DigestAuthSupplier.parseHeader(fullHeader);
        assertEquals(origNonce, map.get("nonce"));
        assertEquals("auth", map.get("qop"));
        assertEquals("MyCompany realm.", map.get("realm"));
    }

    /**
     * Verfies one of the tenents of our interface -- the Conduit sets up
     * an OutputStream on the message after a "prepare".
     */
    @Test
    public void testConduitOutputStream() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        Message message = getNewMessage();

        // Test call
        conduit.prepare(message);

        assertNotNull("Conduit should always set output stream.",
                        message.getContent(OutputStream.class));
    }

    @Test
    public void testAuthPolicyFromEndpointInfo() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        AuthorizationPolicy ap = new AuthorizationPolicy();
        ap.setPassword("password");
        ap.setUserName("testUser");
        ei.addExtensor(ap);
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
        Message message = getNewMessage();

        // Test call
        conduit.prepare(message);

        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertNotNull("Authorization Header should exist",
                headers.get("Authorization"));

        assertEquals("Unexpected Authorization Token",
                "Basic " + Base64Utility.encode("testUser:password".getBytes()),
                headers.get("Authorization").get(0));
    }

    /**
     * This test verifies the precidence of Authorization Information.
     * Setting authorization information on the Message takes precidence
     * over a Basic Auth Supplier with preemptive UserPass, and that
     * followed by setting it directly on the Conduit.
     */
    @Test
    public void testAuthPolicyPrecidence() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        conduit.getAuthorization().setUserName("Satan");
        conduit.getAuthorization().setPassword("hell");
        Message message = getNewMessage();

        // Test call
        conduit.prepare(message);

        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertNotNull("Authorization Header should exist",
                headers.get("Authorization"));

        assertEquals("Unexpected Authorization Token",
                "Basic " + Base64Utility.encode("Satan:hell".getBytes()),
                headers.get("Authorization").get(0));

        // Setting a Basic Auth User Pass should override
        conduit.setAuthSupplier(new BasicAuthSupplier());
        message = getNewMessage();

        // Test Call
        conduit.prepare(message);

        headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertEquals("Unexpected Authorization Token",
                "Basic " + Base64Utility.encode("Gandalf:staff".getBytes()),
                headers.get("Authorization").get(0));

        // Setting authorization policy on the message should override all.
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("Hello");
        authPolicy.setPassword("world");
        authPolicy.setAuthorizationType("Basic");
        message = getNewMessage();
        message.put(AuthorizationPolicy.class, authPolicy);

        conduit.prepare(message);

        headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        assertEquals("Unexpected Authorization Token",
                "Basic " + Base64Utility.encode("Hello:world".getBytes()),
                headers.get("Authorization").get(0));
    }


}
