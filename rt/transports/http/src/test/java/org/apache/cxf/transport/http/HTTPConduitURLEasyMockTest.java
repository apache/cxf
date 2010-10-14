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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.io.AbstractThresholdOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class HTTPConduitURLEasyMockTest extends Assert {
    
    private enum ResponseStyle { NONE, BACK_CHANNEL, DECOUPLED };
    private enum ResponseDelimiter { LENGTH, CHUNKED, EOF };

    private static final String NOWHERE = "http://nada.nothing.nowhere.null/";
    private static final String PAYLOAD = "message payload";
    private IMocksControl control;
    private EndpointInfo endpointInfo;
    private HttpURLConnectionFactory connectionFactory;
    private HttpURLConnection connection;
    private Proxy proxy;
    private Message inMessage;
    private MessageObserver observer;
    private OutputStream os;
    private InputStream is;
    
    /**
     * This is an extension to the HTTPConduit that replaces
     * the dynamic assignment of the HttpURLConnectionFactory,
     * and we just use the EasyMocked version for this test.
     */
    private class HTTPTestConduit extends HTTPConduit {
        HTTPTestConduit(
            Bus                      associatedBus, 
            EndpointInfo             endpoint, 
            EndpointReferenceType    epr,
            HttpURLConnectionFactory testFactory
        ) throws IOException {
            super(associatedBus, endpoint, epr);
            connectionFactory = testFactory;
        }
        @Override
        protected HttpURLConnectionFactory retrieveConnectionFactory(String s) {
            // do nothing. i.e do not change the connectionFactory field.
            return connectionFactory;
        }
        
        
    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // avoid intermittent spurious failures on EasyMock detecting finalize
        // calls by mocking up only class data members (no local variables)
        // and explicitly making available for GC post-verify
        connectionFactory = null;
        connection = null;
        proxy = null;
        inMessage = null;
        observer = null;
        os = null;
        is = null;
    }


    @Test
    public void testSend() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false);
        Message message = new MessageImpl();
        conduit.prepare(message);
        verifySentMessage(conduit, message, "POST");
        finalVerify();
    }
    
    @Test
    public void testSendWithHeaders() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false);
        Message message = new MessageImpl();
        message.put("Content-Type", "text/xml;charset=utf8");
        setUpHeaders(message);
        conduit.prepare(message);
        verifySentMessage(conduit, message, true, "POST");
        finalVerify();
    }
    
    @Test
    public void testSendHttpConnection() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false);
        Message message = new MessageImpl();
        conduit.prepare(message);
        verifySentMessage(conduit, message, "POST");
        finalVerify();
    }

    @Test
    public void testSendHttpConnectionAutoRedirect() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, true);
        Message message = new MessageImpl();
        conduit.prepare(message);
        verifySentMessage(conduit, message, "POST");
        finalVerify();
    }

    @Test
    public void testSendHttpGetConnectionAutoRedirect() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, true, "GET");
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        conduit.prepare(message);
        verifySentMessage(conduit, message, "GET");
        conduit.close(message);
        finalVerify();
    }

    @Test
    public void testSendHttpGetConnection() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "GET");
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        conduit.prepare(message);
        verifySentMessage(conduit, message, "GET");
        conduit.close(message);
        finalVerify();
    }

    @Test
    public void testSendOnewayChunkedEmptyPartialResponse() 
        throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false);
        Message message = new MessageImpl();
        conduit.prepare(message);
        verifySentMessage(conduit, 
                          message, 
                          ResponseStyle.NONE,
                          ResponseDelimiter.CHUNKED,
                          true,  // empty response
                          "POST");
        finalVerify();
    }
    
    private void setUpHeaders(Message message) {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> contentTypes = new ArrayList<String>();
        contentTypes.add("text/xml;charset=utf8");
        headers.put("content-type", contentTypes);
        
        List<String> acceptTypes = new ArrayList<String>();
        acceptTypes.add("text/xml;charset=utf8");
        acceptTypes.add("text/plain");
        headers.put("Accept", acceptTypes);
        
        message.put(Message.PROTOCOL_HEADERS, headers);
        
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("BJ");
        authPolicy.setPassword("value");
        message.put(AuthorizationPolicy.class, authPolicy);        
    }

    private void setUpOneway(Message message) {
        Exchange exchange = control.createMock(Exchange.class);
        message.setExchange(exchange);
        exchange.isOneWay();
        EasyMock.expectLastCall().andReturn(true);
        exchange.isSynchronous();
        EasyMock.expectLastCall().andReturn(true);
    }
    
    private HTTPConduit setUpConduit(boolean send, boolean autoRedirect) throws Exception {
        return setUpConduit(send, autoRedirect, "POST");
    }

    private HTTPConduit setUpConduit(
        boolean send,
        boolean autoRedirect,
        String method
    ) throws Exception {
        endpointInfo = new EndpointInfo();
        endpointInfo.setAddress(NOWHERE + "bar/foo");
        connectionFactory = 
            control.createMock(HttpURLConnectionFactory.class);
        
        if (send) {
            //proxy = control.createMock(Proxy.class);
            proxy =  null;
            connection =
                control.createMock(HttpURLConnection.class);
            connectionFactory.createConnection(
                                      EasyMock.eq(proxy), 
                                      EasyMock.eq(new URL(NOWHERE + "bar/foo")));
            EasyMock.expectLastCall().andReturn(connection);

            connection.setDoOutput(true);
            EasyMock.expectLastCall();
            
            connection.setRequestMethod(method);
            EasyMock.expectLastCall();

            if (!autoRedirect && "POST".equals(method)) {
                connection.setChunkedStreamingMode(-1);
                EasyMock.expectLastCall();
            }
            connection.getRequestMethod();
            EasyMock.expectLastCall().andReturn(method).anyTimes();
            
            connection.setInstanceFollowRedirects(false);
            EasyMock.expectLastCall().times(1);

            connection.setConnectTimeout(303030);
            EasyMock.expectLastCall();
            connection.setReadTimeout(404040);
            EasyMock.expectLastCall();
            connection.setUseCaches(false);
            EasyMock.expectLastCall();
            
        }

        CXFBusImpl bus = new CXFBusImpl();
        
        control.replay();
        
        HTTPConduit conduit = new HTTPTestConduit(bus, 
                                              endpointInfo,
                                              null,
                                              connectionFactory);
        conduit.finalizeConfig();

        if (send) {
            conduit.getClient().setConnectionTimeout(303030);
            conduit.getClient().setReceiveTimeout(404040);
            conduit.getClient().setAutoRedirect(autoRedirect);
            if (!autoRedirect) {
                conduit.getClient().setAllowChunking(true);
                conduit.getClient().setChunkingThreshold(0);
            }
        }

        observer = new MessageObserver() {
            public void onMessage(Message m) {
                inMessage = m;
            }
        };
        conduit.setMessageObserver(observer);
        return conduit;
    }

    private void verifySentMessage(HTTPConduit conduit, Message message, String method)
        throws IOException {
        verifySentMessage(conduit, message, false, method);
    }

    private void verifySentMessage(HTTPConduit conduit,
                                   Message message,
                                   boolean expectHeaders,
                                   String method)
        throws IOException {
        verifySentMessage(conduit,
                          message,
                          expectHeaders, 
                          ResponseStyle.BACK_CHANNEL,
                          method);
    }

    private void verifySentMessage(HTTPConduit conduit,
                                   Message message,
                                   boolean expectHeaders,
                                   ResponseStyle style,
                                   String method)
        throws IOException {
        verifySentMessage(conduit,
                          message,
                          expectHeaders,
                          style,
                          ResponseDelimiter.LENGTH,
                          false,
                          method);
    }
    
    private void verifySentMessage(HTTPConduit conduit,
                                   Message message,
                                   ResponseStyle style,
                                   ResponseDelimiter delimiter,
                                   boolean emptyResponse,
                                   String method)
        throws IOException {
        verifySentMessage(conduit,
                          message,
                          false,
                          style,
                          delimiter,
                          emptyResponse,
                          method);
    }

    private void verifySentMessage(HTTPConduit conduit,
                                   Message message,
                                   boolean expectHeaders,
                                   ResponseStyle style,
                                   ResponseDelimiter delimiter,
                                   boolean emptyResponse,
                                   String method)
        throws IOException {
        control.verify();
        control.reset();

        OutputStream wrappedOS = verifyRequestHeaders(message, expectHeaders, method);

        if (!"GET".equals(method)) {
            os.write(PAYLOAD.getBytes(), 0, PAYLOAD.length());
            EasyMock.expectLastCall();
            
            os.flush();
            EasyMock.expectLastCall();
            os.flush();
            EasyMock.expectLastCall();
            os.close();
            EasyMock.expectLastCall();
        }
        
        if (style == ResponseStyle.NONE) {
            setUpOneway(message);
        }
        
        connection.getRequestMethod();
        EasyMock.expectLastCall().andReturn(method).anyTimes();
        verifyHandleResponse(style, delimiter, conduit);

        control.replay();
        
        wrappedOS.flush();
        wrappedOS.flush();
        wrappedOS.close();

        assertNotNull("expected in message", inMessage);
        Map<?, ?> headerMap = (Map<?, ?>) inMessage.get(Message.PROTOCOL_HEADERS);
        assertEquals("unexpected response headers", headerMap.size(), 0);
        Integer expectedResponseCode = style == ResponseStyle.BACK_CHANNEL
                                       ? HttpURLConnection.HTTP_OK
                                       : HttpURLConnection.HTTP_ACCEPTED;
        assertEquals("unexpected response code",
                     expectedResponseCode,
                     inMessage.get(Message.RESPONSE_CODE));
        if (!emptyResponse) {
            assertTrue("unexpected content formats",
                       inMessage.getContentFormats().contains(InputStream.class));
            InputStream content = inMessage.getContent(InputStream.class);
            if (!(content instanceof PushbackInputStream)) {
                assertSame("unexpected content", is, content);            
            }
        }
        
        finalVerify();
    }

    private OutputStream verifyRequestHeaders(Message message, boolean expectHeaders, String method)
        throws IOException {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        assertNotNull("expected request headers set", headers);
        assertTrue("expected output stream format",
                   message.getContentFormats().contains(OutputStream.class));
        
        connection.getRequestMethod();
        EasyMock.expectLastCall().andReturn(method).anyTimes();

        if (!"GET".equals(method)) {
            os = EasyMock.createMock(OutputStream.class);
            connection.getOutputStream();
            EasyMock.expectLastCall().andReturn(os);
        }
        
        message.put(HTTPConduit.KEY_HTTP_CONNECTION, connection);
        if (expectHeaders) {
            connection.setRequestProperty(EasyMock.eq("Authorization"),
                                          EasyMock.eq("Basic Qko6dmFsdWU="));            
            EasyMock.expectLastCall();
            connection.setRequestProperty(EasyMock.eq("Content-Type"),
                                          EasyMock.eq("text/xml;charset=utf8"));
            EasyMock.expectLastCall();
            connection.setRequestProperty(EasyMock.eq("Accept"),
                                          EasyMock.eq("text/xml;charset=utf8,text/plain"));
            EasyMock.expectLastCall();
        }
        connection.getRequestProperties();
        EasyMock.expectLastCall().andReturn(new HashMap<String, List<String>>()).anyTimes();
        
        control.replay();
        
        AbstractThresholdOutputStream wrappedOS 
            = (AbstractThresholdOutputStream) message.getContent(OutputStream.class);
        assertNotNull("expected output stream", wrappedOS);
        
        wrappedOS.write(PAYLOAD.getBytes());
        wrappedOS.unBuffer();
        
        control.verify();
        control.reset();

        return wrappedOS;
    }
    
    private void verifyHandleResponse(ResponseStyle style, ResponseDelimiter delimiter, HTTPConduit conduit) 
        throws IOException {
        verifyHandleResponse(style, delimiter, false, conduit);
    }
    
    private void verifyHandleResponse(ResponseStyle style, 
                                      ResponseDelimiter delimiter,
                                      boolean emptyResponse,
                                      HTTPConduit conduit) throws IOException {
        connection.getHeaderFields();
        EasyMock.expectLastCall().andReturn(Collections.EMPTY_MAP).anyTimes();
        int responseCode = style == ResponseStyle.BACK_CHANNEL
                           ? HttpURLConnection.HTTP_OK
                           : HttpURLConnection.HTTP_ACCEPTED;
        if (conduit.getClient().isAutoRedirect()) {
            connection.getResponseCode();
            EasyMock.expectLastCall().andReturn(301).once().andReturn(responseCode).anyTimes();
            connection.getURL();
            EasyMock.expectLastCall().andReturn(new URL(NOWHERE + "bar/foo/redirect")).once();
        } else {
            connection.getResponseCode();
            EasyMock.expectLastCall().andReturn(responseCode).anyTimes();
        }
        is = EasyMock.createMock(InputStream.class);
        connection.getInputStream();
        EasyMock.expectLastCall().andReturn(is).anyTimes();
        switch (style) {
        case NONE:            
        case DECOUPLED:
            connection.getContentLength();
            if (delimiter == ResponseDelimiter.CHUNKED 
                || delimiter == ResponseDelimiter.EOF) {
                EasyMock.expectLastCall().andReturn(-1).anyTimes();
                if (delimiter == ResponseDelimiter.CHUNKED) {
                    connection.getHeaderField("Transfer-Encoding");
                    EasyMock.expectLastCall().andReturn("chunked");
                } else if (delimiter == ResponseDelimiter.EOF) {
                    connection.getHeaderField("Connection");
                    EasyMock.expectLastCall().andReturn("close");
                }
                is.read();
                EasyMock.expectLastCall().andReturn(emptyResponse ? -1 : (int)'<');
            } else {
                EasyMock.expectLastCall().andReturn(123).anyTimes();
            }
            if (emptyResponse) {
                is.close();
                EasyMock.expectLastCall();
            }
            break;
            
        case BACK_CHANNEL:
            connection.getErrorStream();
            EasyMock.expectLastCall().andReturn(null);
            break;
            
        default:
            break;
        }
    }
    
    private void finalVerify() {
        if (control != null) {
            control.verify();
            control = null;
        }
    }
    
}
