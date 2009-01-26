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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.message.Message.DECOUPLED_CHANNEL_MESSAGE;


public class HTTPConduitTest extends Assert {
    private Message inMessage;
    private IMocksControl control;

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
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
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

    public void testDecoupledEndpoint() throws Exception {
        control = EasyMock.createNiceControl();

        Bus bus = new CXFBusImpl();

        URL decoupledURL = new URL("http://nowhere.com/response");
        DestinationFactoryManager mgr =
            control.createMock(DestinationFactoryManager.class);
        DestinationFactory factory =
            control.createMock(DestinationFactory.class);
        Destination destination =
            control.createMock(Destination.class);

        bus.setExtension(mgr, DestinationFactoryManager.class);
        mgr.getDestinationFactoryForUri(decoupledURL.toString());
        EasyMock.expectLastCall().andReturn(factory);
        factory.getDestination(EasyMock.isA(EndpointInfo.class));
        EasyMock.expectLastCall().andReturn(destination);
        destination.setMessageObserver(
                EasyMock.isA(HTTPConduit.InterposedMessageObserver.class));

        control.replay();

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();

        // Test call
        conduit.getClient().setDecoupledEndpoint(decoupledURL.toString());

        assertNotNull("expected back channel", conduit.getBackChannel());

        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                inMessage = m;
            }
        };

        // Test call
        conduit.setMessageObserver(observer);

        Message incoming = new MessageImpl();
        conduit.getDecoupledObserver().onMessage(incoming);

        assertSame("expected pass thru onMessage() notification",
                   inMessage,
                   incoming);
        assertEquals("unexpected response code",
                     HttpURLConnection.HTTP_OK,
                     inMessage.get(Message.RESPONSE_CODE));
        assertEquals("expected DECOUPLED_CHANNEL_MESSAGE flag set",
                     Boolean.TRUE,
                     inMessage.get(DECOUPLED_CHANNEL_MESSAGE));
        assertEquals("unexpected HTTP_REQUEST set",
                     false,
                     inMessage.containsKey(AbstractHTTPDestination.HTTP_REQUEST));
        assertEquals("unexpected HTTP_RESPONSE set",
                     false,
                     inMessage.containsKey(AbstractHTTPDestination.HTTP_RESPONSE));
        assertEquals("unexpected Message.ASYNC_POST_RESPONSE_DISPATCH set",
                     false,
                     inMessage.containsKey(Message.ASYNC_POST_RESPONSE_DISPATCH));

        // avoid intermittent spurious failures on EasyMock detecting finalize
        // calls by mocking up only class data members (no local variables)
        // and explicitly making available for GC post-verify
        finalVerify();
        inMessage = null;
    }

    private void finalVerify() {
        if (control != null) {
            control.verify();
            control = null;
        }
    }

}
