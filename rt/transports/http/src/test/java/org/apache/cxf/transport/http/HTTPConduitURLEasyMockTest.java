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


import java.io.ByteArrayInputStream;
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
import java.util.TreeMap;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.io.AbstractThresholdOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 */
public class HTTPConduitURLEasyMockTest {
    private static String oldHttpProxyHost;

    private static final String HTTP_RESPONSE_MESSAGE = "Some Response Message I can test";
    
    private enum ResponseStyle { NONE, BACK_CHANNEL, BACK_CHANNEL_ERROR, DECOUPLED, ONEWAY_NONE };
    private enum ResponseDelimiter { LENGTH, CHUNKED, EOF };

    private static final String NOWHERE = "http://nada.nothing.nowhere.null/";
    private static final String PAYLOAD = "message payload";
    private IMocksControl control;
    private EndpointInfo endpointInfo;
    private HttpsURLConnectionFactory connectionFactory;
    private HttpURLConnection connection;
    private Proxy proxy;
    private Message inMessage;
    private MessageObserver observer;
    private OutputStream os;
    private InputStream is;

    @BeforeClass
    public static void disableHttpProxy() throws Exception {
        oldHttpProxyHost = System.getProperty("http.proxyHost");
        if (oldHttpProxyHost != null) {
            // disable http proxy so that the connection mocking works (see setUpConduit)
            System.clearProperty("http.proxyHost");
        }
    }

    @AfterClass
    public static void revertHttpProxy() throws Exception {
        if (oldHttpProxyHost != null) {
            System.setProperty("http.proxyHost", oldHttpProxyHost);
        }
    }

    /**
     * This is an extension to the HTTPConduit that replaces
     * the dynamic assignment of the HttpURLConnectionFactory,
     * and we just use the EasyMocked version for this test.
     */
    private class HTTPTestConduit extends URLConnectionHTTPConduit {
        HTTPTestConduit(
            Bus                      associatedBus,
            EndpointInfo             endpoint,
            EndpointReferenceType    epr,
            HttpsURLConnectionFactory testFactory
        ) throws IOException {
            super(associatedBus, endpoint, epr);
            connectionFactory = testFactory;
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
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = createMessage();
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.FALSE);
        conduit.prepare(message);
        verifySentMessage(conduit, message, "POST");
        assertNull(inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        finalVerify();
    }

    @Test
    public void testSendWithHeaders() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = createMessage();
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.TRUE);
        setUpHeaders(message);
        conduit.prepare(message);
        verifySentMessage(conduit, message, true, "POST", false);
        assertEquals(HTTP_RESPONSE_MESSAGE, inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        finalVerify();
    }

    private Message createMessage() {
        Message message = new MessageImpl();
        message.put("Content-Type", "text/xml;charset=utf8");
        message.setContent(List.class, new MessageContentsList("<body/>"));
        return message;
    }

    @Test
    @org.junit.Ignore
    public void testSendWithHeadersCheckErrorStream() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = new MessageImpl();
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.TRUE);
        message.put("Content-Type", "text/xml;charset=utf8");
        setUpHeaders(message);
        conduit.prepare(message);
        verifySentMessage(conduit, message, true, "POST", true);
        assertEquals(HTTP_RESPONSE_MESSAGE, inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        finalVerify();
    }

    @Test
    public void testSendHttpConnection() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = createMessage();
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.TRUE);
        conduit.prepare(message);
        verifySentMessage(conduit, message, "POST");
        assertEquals(HTTP_RESPONSE_MESSAGE, inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        finalVerify();
    }

    @Test
    public void testSendHttpConnectionAutoRedirect() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, true, "POST");
        Message message = createMessage();
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.TRUE);
        conduit.prepare(message);
        verifySentMessage(conduit, message, "POST");
        assertEquals(HTTP_RESPONSE_MESSAGE, inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        finalVerify();
    }

    @Test
    public void testSendHttpGetConnectionAutoRedirect() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, true, "GET");
        Message message = new MessageImpl();
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.TRUE);
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        conduit.prepare(message);
        verifySentMessage(conduit, message, "GET");
        assertEquals(HTTP_RESPONSE_MESSAGE, inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        conduit.close(message);
        finalVerify();
    }

    @Test
    public void testSendHttpGetConnection() throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "GET");
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, Boolean.TRUE);
        conduit.prepare(message);
        verifySentMessage(conduit, message, "GET");
        assertEquals(HTTP_RESPONSE_MESSAGE, inMessage.get(HTTPConduit.HTTP_RESPONSE_MESSAGE));
        conduit.close(message);
        finalVerify();
    }

    @Test
    public void testSendOnewayChunkedEmptyPartialResponseProcessResponse()
        throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = createMessage();
        conduit.prepare(message);
        message.put(Message.PROCESS_ONEWAY_RESPONSE, Boolean.TRUE);
        verifySentMessage(conduit,
                          message,
                          ResponseStyle.NONE,
                          ResponseDelimiter.CHUNKED,
                          true,  // empty response
                          "POST");
        finalVerify();
    }

    @Test
    public void testSendOnewayDoNotProcessResponse()
        throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = createMessage();
        conduit.prepare(message);
        verifySentMessage(conduit,
                          message,
                          ResponseStyle.ONEWAY_NONE,
                          ResponseDelimiter.CHUNKED,
                          true,  // empty response
                          "POST");
        finalVerify();
    }

    @Test
    public void testSendTwowayDecoupledEmptyPartialResponse()
        throws Exception {
        control = EasyMock.createNiceControl();
        HTTPConduit conduit = setUpConduit(true, false, "POST");
        Message message = createMessage();
        conduit.prepare(message);
        verifySentMessage(conduit,
                          message,
                          ResponseStyle.DECOUPLED,
                          ResponseDelimiter.EOF,
                          true,  // empty response
                          "POST");
        finalVerify();
    }

    private void setUpHeaders(Message message) {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String> contentTypes = new ArrayList<>();
        contentTypes.add("text/xml;charset=utf8");
        headers.put("content-type", contentTypes);

        List<String> acceptTypes = new ArrayList<>();
        acceptTypes.add("text/xml;charset=utf8");
        acceptTypes.add("text/plain");
        headers.put("Accept", acceptTypes);

        message.put(Message.PROTOCOL_HEADERS, headers);

        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("BJ");
        authPolicy.setPassword("value");
        message.put(AuthorizationPolicy.class, authPolicy);
    }

    private void setUpExchange(Message message, boolean oneway) {
        Exchange exchange = control.createMock(Exchange.class);
        message.setExchange(exchange);
        exchange.isOneWay();
        EasyMock.expectLastCall().andReturn(oneway).anyTimes();
        exchange.isSynchronous();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        exchange.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
    }

    private HTTPConduit setUpConduit(
        boolean send,
        boolean autoRedirect,
        String method) throws Exception {
        endpointInfo = new EndpointInfo();
        endpointInfo.setAddress(NOWHERE + "bar/foo");
        connectionFactory =
            control.createMock(HttpsURLConnectionFactory.class);

        if (send) {
            //proxy = control.createMock(Proxy.class);
            proxy = Proxy.NO_PROXY;
            connection =
                control.createMock(HttpURLConnection.class);
            connection.getURL();
            EasyMock.expectLastCall().andReturn(new URL(NOWHERE + "bar/foo")).anyTimes();

            connectionFactory.createConnection((TLSClientParameters)EasyMock.isNull(),
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

        ExtensionManagerBus bus = new ExtensionManagerBus();

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
        verifySentMessage(conduit, message, false, method, false);
    }

    private void verifySentMessage(HTTPConduit conduit,
                                   Message message,
                                   boolean expectHeaders,
                                   String method,
                                   boolean errorExpected)
        throws IOException {
        verifySentMessage(conduit,
                          message,
                          expectHeaders,
                          errorExpected ? ResponseStyle.BACK_CHANNEL_ERROR : ResponseStyle.BACK_CHANNEL,
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

        setUpExchange(message, style == ResponseStyle.NONE || style == ResponseStyle.ONEWAY_NONE);

        connection.getRequestMethod();
        EasyMock.expectLastCall().andReturn(method).anyTimes();
        verifyHandleResponse(style, delimiter, emptyResponse, conduit);

        control.replay();

        wrappedOS.flush();
        wrappedOS.flush();
        wrappedOS.close();

        if ((style == ResponseStyle.NONE && !emptyResponse)
            || style == ResponseStyle.BACK_CHANNEL
            || style == ResponseStyle.BACK_CHANNEL_ERROR) {
            assertNotNull("expected in message", inMessage);
            Map<?, ?> headerMap = (Map<?, ?>) inMessage.get(Message.PROTOCOL_HEADERS);
            assertEquals("unexpected response headers", headerMap.size(), 0);
            Integer expectedResponseCode = getResponseCode(style);
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

    private void verifyHandleResponse(ResponseStyle style,
                                      ResponseDelimiter delimiter,
                                      boolean emptyResponse,
                                      HTTPConduit conduit) throws IOException {
        connection.getHeaderFields();
        EasyMock.expectLastCall().andReturn(Collections.EMPTY_MAP).anyTimes();
        
        connection.getResponseMessage();
        EasyMock.expectLastCall().andReturn(HTTP_RESPONSE_MESSAGE).anyTimes();
        
        int responseCode = getResponseCode(style);
        if (conduit.getClient().isAutoRedirect()) {
            connection.getResponseCode();
            EasyMock.expectLastCall().andReturn(301).once().andReturn(responseCode).anyTimes();
            connection.getURL();
            EasyMock.expectLastCall().andReturn(new URL(NOWHERE + "bar/foo/redirect")).anyTimes();
        } else {
            connection.getResponseCode();
            EasyMock.expectLastCall().andReturn(responseCode).anyTimes();
        }

        switch (style) {
        case NONE:
        case DECOUPLED:
            is = control.createMock(InputStream.class);
            connection.getInputStream();
            EasyMock.expectLastCall().andReturn(is).anyTimes();
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
                if (emptyResponse) {
                    EasyMock.expectLastCall().andReturn(-1).anyTimes();
                } else {
                    EasyMock.expectLastCall().andReturn((int)'<');
                }
            } else {
                EasyMock.expectLastCall().andReturn(123).anyTimes();
            }
            if (emptyResponse) {
                is.close();
                EasyMock.expectLastCall();
            }
            break;

        case BACK_CHANNEL:
            is = EasyMock.createMock(InputStream.class);
            connection.getInputStream();
            EasyMock.expectLastCall().andReturn(is).anyTimes();
            break;

        case BACK_CHANNEL_ERROR:
            is = EasyMock.createMock(InputStream.class);
            connection.getInputStream();
            EasyMock.expectLastCall().andReturn(is).anyTimes();
            connection.getErrorStream();
            EasyMock.expectLastCall().andReturn(null);
            break;

        case ONEWAY_NONE:
            connection.getInputStream();
            EasyMock.expectLastCall().andReturn(new ByteArrayInputStream(new byte[0])).anyTimes();
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

    private int getResponseCode(ResponseStyle style) {
        int code;
        if (style == ResponseStyle.BACK_CHANNEL) {
            code = HttpURLConnection.HTTP_OK;
        } else if (style == ResponseStyle.BACK_CHANNEL_ERROR) {
            code = HttpURLConnection.HTTP_BAD_REQUEST;
        } else {
            code = HttpURLConnection.HTTP_ACCEPTED;
        }
        return code;
    }
}