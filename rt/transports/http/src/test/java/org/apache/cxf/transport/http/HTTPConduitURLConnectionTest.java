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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This class "tests" the HTTPConduit that uses java.net.HttpURLConnection
 * and java.net.HttpsURLConnection for its implementation. Should the
 * implementation of HTTPConduit change (i.e. no longer use the URLConnections)
 * this test will break.
 */
public class HTTPConduitURLConnectionTest extends Assert {
    
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
     * This test verifies that the "prepare" call places an HttpURLConnection on 
     * the Message and that its URL matches the endpoint.
     */
    @Test
    public void testConnectionURL() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
    
        Message message = getNewMessage();
        
        conduit.prepare(message);
        
        HttpURLConnection con = 
            (HttpURLConnection) message.get("http.connection");
        assertEquals("Unexpected URL address",
                con.getURL().toString(),
                ei.getAddress());
    }

    /**
     * This test verifies that URL used is overridden by having the 
     * ENDPOINT_ADDRESS set on the Message.
     */
    @Test
    public void testConnectionURLOverride() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.null/bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
    
        Message message = getNewMessage();
        message.put(Message.ENDPOINT_ADDRESS, "http://somewhere.different/");
        
        // Test call
        conduit.prepare(message);
        
        HttpURLConnection con = 
            (HttpURLConnection) message.get("http.connection");
        assertEquals("Unexpected URL address",
                con.getURL().toString(),
                "http://somewhere.different/");
    }

    /**
     * This verifys that the underlying connection is an HttpsURLConnection.
     */
    @Test
    public void testTLSServerParameters() throws Exception {
        Object connection = doTestTLSServerParameters();
        assertTrue("TLS Client Parameters should generate an HttpsURLConnection",
                HttpsURLConnection.class.isInstance(connection));
    }

    /**
     * This verifys that the underlying connection is an HttpsURLConnection.
     */
    @Test
    public void testTLSServerParametersWithDeprecatedSunSSLProtocol() throws Exception {
        if (!System.getProperty("java.vm.vendor").toLowerCase().contains("sun")) {
            return;
        }
        String javaProtocolHandlerPkgsKey = "java.protocol.handler.pkgs";
        String javaProtocolHandlerPkgsValue = System.getProperty(javaProtocolHandlerPkgsKey);
        try {
            System.setProperty(javaProtocolHandlerPkgsKey, "com.sun.net.ssl.internal.www.protocol");
            Object connection = doTestTLSServerParameters();
            assertTrue("TLS Client Parameters should generate an HttpsURLConnection",
                    connection.getClass().getName().contains("HttpsURLConnection"));
        } finally {
            if (javaProtocolHandlerPkgsValue == null) {
                System.clearProperty(javaProtocolHandlerPkgsKey);
            } else {
                System.setProperty(javaProtocolHandlerPkgsKey, javaProtocolHandlerPkgsValue);
            }
        }
    }
    
    private Object doTestTLSServerParameters() throws Exception {
        Bus bus = new CXFBusImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("https://secure.nowhere.null/" + "bar/foo");
        HTTPConduit conduit = new HTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
    
        Message message = getNewMessage();
        // We need an SSL policy, or we can't use "https".
        conduit.setTlsClientParameters(new TLSClientParameters());
        
        // Test call
        conduit.prepare(message);
        
        return message.get("http.connection");
    }


}
